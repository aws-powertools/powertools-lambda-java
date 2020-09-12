package software.amazon.lambda.powertools.metrics;

import java.util.function.Consumer;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;

public final class PowertoolsMetricsLogger {
    private static MetricsLogger metricsLogger = new MetricsLogger();

    public static MetricsLogger metricsLogger() {
        return metricsLogger;
    }

    public static void withSingleMetric(final String name,
                                        final double value,
                                        final Unit unit,
                                        final String namespace,
                                        final Consumer<MetricsLogger> logger) {
        MetricsLogger metricsLogger = new MetricsLogger();
        try {
            metricsLogger.setNamespace(namespace);
            metricsLogger.putMetric(name, value, unit);
            logger.accept(metricsLogger);
        } finally {
            metricsLogger.flush();
        }
    }
}
