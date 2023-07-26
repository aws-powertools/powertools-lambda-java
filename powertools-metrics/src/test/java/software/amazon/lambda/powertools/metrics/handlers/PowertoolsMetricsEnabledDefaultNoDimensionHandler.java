package software.amazon.lambda.powertools.metrics.handlers;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.withSingleMetric;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

public class PowertoolsMetricsEnabledDefaultNoDimensionHandler implements RequestHandler<Object, Object> {

    static {
        MetricsUtils.defaultDimensions();
    }

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        metricsLogger.putMetric("Metric1", 1, Unit.BYTES);

        withSingleMetric("Metric2", 1, Unit.COUNT, log ->
            {
            });

        return null;
    }
}
