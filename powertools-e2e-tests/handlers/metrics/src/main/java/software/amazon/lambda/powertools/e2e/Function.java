package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

public class Function implements RequestHandler<Input, String> {

    MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    @Metrics(captureColdStart = true)
    public String handleRequest(Input input, Context context) {

        DimensionSet dimensionSet = new DimensionSet();
        input.getDimensions().forEach((key, value) -> dimensionSet.addDimension(key, value));
        metricsLogger.putDimensions(dimensionSet);

        input.getMetrics().forEach((key, value) -> metricsLogger.putMetric(key, value, Unit.COUNT));

        return "OK";
    }
}