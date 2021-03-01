package software.amazon.cloudwatchlogs.emf.model;

import java.lang.reflect.Field;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

public final class MetricsLoggerHelper {
    private MetricsLoggerHelper() {
    }

    public static boolean hasNoMetrics() {
        return metricsContext().getRootNode().getAws().isEmpty();
    }

    public static long dimensionsCount() {
        return metricsContext().getDimensions().size();
    }

    public static MetricsContext metricsContext() {
        try {
            Field f = metricsLogger().getClass().getDeclaredField("context");
            f.setAccessible(true);
            return (MetricsContext) f.get(metricsLogger());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
