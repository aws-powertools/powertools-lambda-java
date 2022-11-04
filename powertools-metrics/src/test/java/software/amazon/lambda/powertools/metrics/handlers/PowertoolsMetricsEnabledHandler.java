package software.amazon.lambda.powertools.metrics.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.withSingleMetric;

public class PowertoolsMetricsEnabledHandler implements RequestHandler<Object, Object> {

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        try {
            metricsLogger.putMetric("Metric1", 1, Unit.BYTES);
        } catch (InvalidMetricException e) {
            throw new RuntimeException(e);
        }

        withSingleMetric("Metric2", 1, Unit.COUNT,
                log -> {
                    try {
                        log.setDimensions(DimensionSet.of("Dimension1", "Value1"));
                    } catch (InvalidDimensionException | DimensionSetExceededException e) {
                        throw new RuntimeException(e);
                    }
                });

        return null;
    }
}
