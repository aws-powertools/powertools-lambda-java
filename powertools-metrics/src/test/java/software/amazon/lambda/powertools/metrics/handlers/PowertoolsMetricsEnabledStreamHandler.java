package software.amazon.lambda.powertools.metrics.handlers;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

public class PowertoolsMetricsEnabledStreamHandler implements RequestStreamHandler {

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        try {
            metricsLogger.putMetric("Metric1", 1, Unit.BYTES);
        } catch (InvalidMetricException e) {
            throw new RuntimeException(e);
        }
    }
}
