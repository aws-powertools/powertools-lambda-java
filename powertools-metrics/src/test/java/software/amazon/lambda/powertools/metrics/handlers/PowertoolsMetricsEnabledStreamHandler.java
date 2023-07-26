package software.amazon.lambda.powertools.metrics.handlers;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;

public class PowertoolsMetricsEnabledStreamHandler implements RequestStreamHandler {

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        metricsLogger.putMetric("Metric1", 1, Unit.BYTES);
    }
}
