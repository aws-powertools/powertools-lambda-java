package software.amazon.lambda.powertools.testutils.tracing;

import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.Status;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.xray.XRayClient;
import software.amazon.awssdk.services.xray.model.*;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;

/**
 * Class in charge of retrieving the actual traces of a Lambda execution on X-Ray
 */
public class TraceFetcher {

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger LOG = LoggerFactory.getLogger(TraceFetcher.class);

    private final Instant start;
    private final Instant end;
    private final String filterExpression;
    private final List<String> excludedSegments;

    /**
     * @param start beginning of the time slot to search in
     * @param end end of the time slot to search in
     * @param filterExpression eventual filter for the search
     * @param excludedSegments list of segment to exclude from the search
     */
    public TraceFetcher(Instant start, Instant end, String filterExpression, List<String> excludedSegments) {
        this.start = start;
        this.end = end;
        this.filterExpression = filterExpression;
        this.excludedSegments = excludedSegments;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve the traces corresponding to a specific function during a specific time slot.
     * Use a retry mechanism as traces may not be available instantaneously after a function runs.
     *
     * @return traces
     */
    public Trace fetchTrace() {
        Callable<Trace> callable = () -> {
            List<String> traceIds = getTraceIds();
            return getTrace(traceIds);
        };

        RetryConfig retryConfig = new RetryConfigBuilder()
                .withMaxNumberOfTries(10)
                .retryOnAnyException()
                .withDelayBetweenTries(ofSeconds(5))
                .withRandomExponentialBackoff()
                .build();
        CallExecutor<Trace> callExecutor = new CallExecutorBuilder<Trace>()
                .config(retryConfig)
                .afterFailedTryListener(s -> {LOG.warn(s.getLastExceptionThatCausedRetry().getMessage() + ", attempts: " + s.getTotalTries());})
                .build();
        Status<Trace> status = callExecutor.execute(callable);
        return status.getResult();
    }

    /**
     * Retrieve traces from trace ids.
     * @param traceIds
     * @return
     */
    private Trace getTrace(List<String> traceIds) {
        BatchGetTracesResponse tracesResponse = xray.batchGetTraces(BatchGetTracesRequest.builder()
                .traceIds(traceIds)
                .build());
        if (!tracesResponse.hasTraces()) {
            throw new RuntimeException("No trace found");
        }
        Trace traceRes = new Trace();
        tracesResponse.traces().forEach(trace -> {
            if (trace.hasSegments()) {
                trace.segments().forEach(segment -> {
                    try {
                        SegmentDocument document = MAPPER.readValue(segment.document(), SegmentDocument.class);
                        if (document.getOrigin().equals("AWS::Lambda::Function")) {
                            if (document.hasSubsegments()) {
                                getNestedSubSegments(document.getSubsegments(), traceRes, Collections.emptyList());
                            }
                        }
                    } catch (JsonProcessingException e) {
                        LOG.error("Failed to parse segment document: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            }
        });
        return traceRes;
    }

    private void getNestedSubSegments(List<SubSegment> subsegments, Trace traceRes, List<String> idsToIgnore) {
        subsegments.forEach(subsegment -> {
            List<String> subSegmentIdsToIgnore = Collections.emptyList();
            if (!excludedSegments.contains(subsegment.getName()) && !idsToIgnore.contains(subsegment.getId())) {
                traceRes.addSubSegment(subsegment);
                if (subsegment.hasSubsegments()) {
                    subSegmentIdsToIgnore = subsegment.getSubsegments().stream().map(SubSegment::getId).collect(Collectors.toList());
                }
            }
            if (subsegment.hasSubsegments()) {
                getNestedSubSegments(subsegment.getSubsegments(), traceRes, subSegmentIdsToIgnore);
            }
        });
    }

    /**
     * Use the X-Ray SDK to retrieve the trace ids corresponding to a specific function during a specific time slot
     * @return a list of trace ids
     */
    private List<String> getTraceIds() {
        GetTraceSummariesResponse traceSummaries = xray.getTraceSummaries(GetTraceSummariesRequest.builder()
                .startTime(start)
                .endTime(end)
                .timeRangeType(TimeRangeType.EVENT)
                .sampling(false)
                .filterExpression(filterExpression)
                .build());
        if (!traceSummaries.hasTraceSummaries()) {
            throw new RuntimeException("No trace id found");
        }
        List<String> traceIds = traceSummaries.traceSummaries().stream().map(TraceSummary::id).collect(Collectors.toList());
        if (traceIds.isEmpty()) {
            throw new RuntimeException("No trace id found");
        }
        return traceIds;
    }

    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
    private static final XRayClient xray = XRayClient.builder()
            .httpClient(httpClient)
                .region(region)
                .build();

    public static class Builder {
        private Instant start;
        private Instant end;
        private String filterExpression;
        private List<String> excludedSegments = List.of("Initialization", "Invocation", "Overhead");

        public TraceFetcher build() {
            if (filterExpression == null)
                throw new IllegalArgumentException("filterExpression or functionName is required");
            if (start == null)
                throw new IllegalArgumentException("start is required");
            if (end == null)
                end = start.plus(1, ChronoUnit.MINUTES);
            LOG.debug("Looking for traces from {} to {} with filter {}", start, end, filterExpression);
            return new TraceFetcher(start, end, filterExpression, excludedSegments);
        }

        public Builder start(Instant start) {
            this.start = start;
            return this;
        }

        public Builder end(Instant end) {
            this.end = end;
            return this;
        }

        public Builder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        /**
         * "Initialization", "Invocation", "Overhead" are excluded by default
         * @param excludedSegments
         * @return
         */
        public Builder excludeSegments(List<String> excludedSegments) {
            this.excludedSegments = excludedSegments;
            return this;
        }

        public Builder functionName(String functionName) {
            this.filterExpression = String.format("service(id(name: \"%s\", type: \"AWS::Lambda::Function\"))", functionName);
            return this;
        }
    }
}
