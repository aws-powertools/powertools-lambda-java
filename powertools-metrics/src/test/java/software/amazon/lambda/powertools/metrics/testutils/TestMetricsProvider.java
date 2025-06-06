package software.amazon.lambda.powertools.metrics.testutils;

import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;

public class TestMetricsProvider implements MetricsProvider {
    @Override
    public Metrics getMetricsInstance() {
        return new TestMetrics();
    }
}
