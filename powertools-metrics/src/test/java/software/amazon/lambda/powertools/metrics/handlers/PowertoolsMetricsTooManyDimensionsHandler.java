package software.amazon.lambda.powertools.metrics.handlers;

import java.util.stream.IntStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.Metrics;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

public class PowertoolsMetricsTooManyDimensionsHandler implements RequestHandler<Object, Object> {

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();



        metricsLogger.setDimensions(IntStream.range(1, 15)
                .mapToObj(value -> {
                    try {
                        return DimensionSet.of("Dimension" + value, "DimensionValue" + value);
                    } catch (InvalidDimensionException | DimensionSetExceededException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(DimensionSet[]::new));

        return null;
    }
}
