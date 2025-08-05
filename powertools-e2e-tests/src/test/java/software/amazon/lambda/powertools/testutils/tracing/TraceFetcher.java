/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.testutils.tracing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.xray.XRayClient;
import software.amazon.awssdk.services.xray.model.BatchGetTracesRequest;
import software.amazon.awssdk.services.xray.model.BatchGetTracesResponse;
import software.amazon.awssdk.services.xray.model.GetTraceSummariesRequest;
import software.amazon.awssdk.services.xray.model.GetTraceSummariesResponse;
import software.amazon.awssdk.services.xray.model.TimeRangeType;
import software.amazon.awssdk.services.xray.model.TraceSummary;
import software.amazon.lambda.powertools.testutils.RetryUtils;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;

/**
 * Class in charge of retrieving the actual traces of a Lambda execution on X-Ray
 */
public class TraceFetcher {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger LOG = LoggerFactory.getLogger(TraceFetcher.class);
    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
    private static final XRayClient xray = XRayClient.builder()
            .httpClient(httpClient)
            .region(region)
            .build();
    private final Instant start;
    private final Instant end;
    private final String filterExpression;
    private final List<String> excludedSegments;

    /**
     * @param start            beginning of the time slot to search in
     * @param end              end of the time slot to search in
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
        Supplier<Trace> supplier = () -> {
            List<String> traceIds = getTraceIds();
            return getTrace(traceIds);
        };

        return RetryUtils.withRetry(supplier, "trace-fetcher", TraceNotFoundException.class).get();
    }

    /**
     * Retrieve traces from trace ids.
     *
     * @param traceIds
     * @return
     */
    private Trace getTrace(List<String> traceIds) {
        BatchGetTracesResponse tracesResponse = xray.batchGetTraces(BatchGetTracesRequest.builder()
                .traceIds(traceIds)
                .build());
        if (!tracesResponse.hasTraces()) {
            throw new TraceNotFoundException("No trace found");
        }
        Trace traceRes = new Trace();
        tracesResponse.traces().forEach(trace -> {
            if (trace.hasSegments()) {
                trace.segments().forEach(segment -> {
                    try {
                        SegmentDocument document = MAPPER.readValue(segment.document(), SegmentDocument.class);
                        if ("AWS::Lambda::Function".equals(document.getOrigin()) && document.hasSubsegments()) {
                            getNestedSubSegments(document.getSubsegments(), traceRes,
                                    Collections.emptyList());
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
                    subSegmentIdsToIgnore = subsegment.getSubsegments().stream().map(SubSegment::getId)
                            .collect(Collectors.toList());
                }
            }
            if (subsegment.hasSubsegments()) {
                getNestedSubSegments(subsegment.getSubsegments(), traceRes, subSegmentIdsToIgnore);
            }
        });
    }

    /**
     * Use the X-Ray SDK to retrieve the trace ids corresponding to a specific function during a specific time slot
     *
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
            throw new TraceNotFoundException("No trace id found");
        }
        List<String> traceIds = traceSummaries.traceSummaries().stream().map(TraceSummary::id)
                .collect(Collectors.toList());
        if (traceIds.isEmpty()) {
            throw new TraceNotFoundException("No trace id found");
        }
        return traceIds;
    }

    public static class Builder {
        private Instant start;
        private Instant end;
        private String filterExpression;
        private List<String> excludedSegments = Arrays.asList("Initialization", "Invocation", "Overhead");

        public TraceFetcher build() {
            if (filterExpression == null) {
                throw new IllegalArgumentException("filterExpression or functionName is required");
            }
            if (start == null) {
                throw new IllegalArgumentException("start is required");
            }
            if (end == null) {
                end = start.plus(1, ChronoUnit.MINUTES);
            }
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
         *
         * @param excludedSegments
         * @return
         */
        public Builder excludeSegments(List<String> excludedSegments) {
            this.excludedSegments = excludedSegments;
            return this;
        }

        public Builder functionName(String functionName) {
            this.filterExpression = String.format("service(id(name: \"%s\", type: \"AWS::Lambda::Function\"))",
                    functionName);
            return this;
        }
    }
}
