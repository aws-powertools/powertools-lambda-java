package software.amazon.lambda.powertools.metrics;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

public final class PowertoolsMetricsLogger {
    private static MetricsLogger metricsLogger = new MetricsLogger();

    public static MetricsLogger logger() {
        return metricsLogger;
    }
}
