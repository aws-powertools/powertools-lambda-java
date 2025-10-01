package software.amazon.lambda.powertools.metrics.internal;

import com.amazonaws.services.lambda.runtime.Context;
import software.amazon.lambda.powertools.metrics.Metrics;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.getXrayTraceId;

final class MetricsUtils {
    private static final String TRACE_ID_PROPERTY = "xray_trace_id";
    private static final String REQUEST_ID_PROPERTY = "function_request_id";

    private MetricsUtils() {
        // Utility class
    }

    static void addRequestIdAndXrayTraceIdIfAvailable(Context context, Metrics metrics) {
        if (context != null && context.getAwsRequestId() != null) {
            metrics.addMetadata(REQUEST_ID_PROPERTY, context.getAwsRequestId());
        }
        getXrayTraceId().ifPresent(traceId -> metrics.addMetadata(TRACE_ID_PROPERTY, traceId));
    }
}
