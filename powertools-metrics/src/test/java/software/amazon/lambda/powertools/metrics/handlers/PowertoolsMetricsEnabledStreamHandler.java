package software.amazon.lambda.powertools.metrics.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

import static software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger.metricsLogger;

public class PowertoolsMetricsEnabledStreamHandler implements RequestStreamHandler {

    @Override
    @PowertoolsMetrics(namespace = "ExampleApplication", service = "booking")
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        metricsLogger.putMetric("Metric1", 1, Unit.BYTES);
    }
}
