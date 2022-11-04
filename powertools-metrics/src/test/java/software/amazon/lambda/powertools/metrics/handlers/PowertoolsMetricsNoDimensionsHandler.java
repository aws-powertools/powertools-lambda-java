package software.amazon.lambda.powertools.metrics.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.Metrics;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

public class PowertoolsMetricsNoDimensionsHandler implements RequestHandler<Object, Object> {

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        try {
            metricsLogger.putMetric("CoolMetric", 1);
        } catch (InvalidMetricException e) {
            throw new RuntimeException(e);
        }
        metricsLogger.setDimensions(new DimensionSet());

        return null;
    }
}
