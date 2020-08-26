package software.amazon.lambda.powertools.metrics.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

public class PowertoolsMetricsEnabledHandler implements RequestHandler<Object, Object> {

    @Override
    @PowertoolsMetrics
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = PowertoolsMetric.getMetricLogger();


        return null;
    }
}
