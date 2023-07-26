package software.amazon.lambda.powertools.metrics.handlers;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.Metrics;

public class PowertoolsMetricsNoExceptionWhenNoMetricsHandler implements RequestHandler<Object, Object> {

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        metricsLogger.putMetadata("MetaData", "MetaDataValue");

        return null;
    }
}
