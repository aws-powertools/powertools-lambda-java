package software.amazon.lambda.powertools.metrics.testutils;

import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;

public class TestMetricsProvider implements MetricsProvider {
    @Override
    public MetricsLogger getMetricsLogger() {
        return new TestMetricsLogger();
    }
}
