package software.amazon.lambda.powertools.metrics.handlers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

import static software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger.metricsLogger;

public class PowertoolsMetricsTooManyDimensionsHandler implements RequestHandler<Object, Object> {

    @Override
    @PowertoolsMetrics
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();

        metricsLogger.setDimensions(IntStream.range(1, 15)
                .mapToObj(value -> DimensionSet.of("Dimension" + value, "DimensionValue" + value))
                .toArray(DimensionSet[]::new));

        return null;
    }
}
