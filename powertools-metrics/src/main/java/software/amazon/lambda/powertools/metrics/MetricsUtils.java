package software.amazon.lambda.powertools.metrics;

import java.util.function.Consumer;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * A class used to retrieve the instance of the {@code MetricsLogger} used by
 * {@code Metrics}.
 *
 * {@see Metrics}
 */
public final class MetricsUtils {
    private static final MetricsLogger metricsLogger = new MetricsLogger();

    private MetricsUtils() {
    }

    /**
     * The instance of the {@code MetricsLogger} used by {@code Metrics}.
     *
     * @return The instance of the MetricsLogger used by Metrics.
     */
    public static MetricsLogger metricsLogger() {
        return metricsLogger;
    }

    /**
     * Add and immediately flush a single metric.
     *
     * @param name the name of the metric
     * @param value the value of the metric
     * @param unit the unit type of the metric
     * @param namespace the namespace associated with the metric
     * @param logger the MetricsLogger
     */
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
