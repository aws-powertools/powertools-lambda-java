package software.amazon.lambda.powertools.metrics;

import java.util.Optional;
import java.util.function.Consumer;

import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.MetricsLoggerHelper;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.metrics.internal.LambdaMetricsAspect.REQUEST_ID_PROPERTY;
import static software.amazon.lambda.powertools.metrics.internal.LambdaMetricsAspect.TRACE_ID_PROPERTY;

/**
 * A class used to retrieve the instance of the {@code MetricsLogger} used by
 * {@code Metrics}.
 *
 * {@see Metrics}
 */
public final class MetricsUtils {
    private static final MetricsLogger metricsLogger = new MetricsLogger();
    private static DimensionSet[] defaultDimensions;

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
     * Configure default dimension to be used by logger.
     * By default, @{@link Metrics} annotation captures configured service as a dimension <i>Service</i>
     * @param dimensionSets Default value of dimensions set for logger
     */
    public static void defaultDimensions(final DimensionSet... dimensionSets) {
        MetricsUtils.defaultDimensions = dimensionSets;
    }

    /**
     * Configure default dimension to be used by logger.
     * By default, @{@link Metrics} annotation captures configured service as a dimension <i>Service</i>
     * @param dimensionSet Default value of dimension set for logger
     * @deprecated use {@link #defaultDimensions(DimensionSet...)} instead
     *
     */
    @Deprecated
    public static void defaultDimensionSet(final DimensionSet dimensionSet) {
        requireNonNull(dimensionSet, "Null dimension set not allowed");

        if(dimensionSet.getDimensionKeys().size() > 0) {
            MetricsUtils.defaultDimensions = new DimensionSet[]{dimensionSet};
        }
    }


    /**
     * Add and immediately flush a single metric. It will use the default namespace
     * specified either on {@link Metrics} annotation or via POWERTOOLS_METRICS_NAMESPACE env var.
     * It by default captures function_request_id as property if used together with {@link Metrics} annotation. It will also
     * capture xray_trace_id as property if tracing is enabled.
     *
     * @param name   the name of the metric
     * @param value  the value of the metric
     * @param unit   the unit type of the metric
     * @param logger the MetricsLogger
     */
    public static void withSingleMetric(final String name,
                                        final double value,
                                        final Unit unit,
                                        final Consumer<MetricsLogger> logger) {
        MetricsLogger metricsLogger = logger();

        try {
            metricsLogger.setNamespace(defaultNameSpace());
            metricsLogger.putMetric(name, value, unit);
            captureRequestAndTraceId(metricsLogger);
            logger.accept(metricsLogger);
        } finally {
            metricsLogger.flush();
        }
    }

    /**
     * Add and immediately flush a single metric.
     * It by default captures function_request_id as property if used together with {@link Metrics} annotation. It will also
     * capture xray_trace_id as property if tracing is enabled.
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
        MetricsLogger metricsLogger = logger();

        try {
            metricsLogger.setNamespace(namespace);
            metricsLogger.putMetric(name, value, unit);
            captureRequestAndTraceId(metricsLogger);
            logger.accept(metricsLogger);
        } finally {
            metricsLogger.flush();
        }
    }

    public static DimensionSet[] defaultDimensions() {
        return defaultDimensions;
    }

    public static boolean hasDefaultDimension() {
        return null != defaultDimensions;
    }

    private static void captureRequestAndTraceId(MetricsLogger metricsLogger) {
        awsRequestId().
                ifPresent(requestId -> metricsLogger.putProperty(REQUEST_ID_PROPERTY, requestId));

        getXrayTraceId()
                .ifPresent(traceId -> metricsLogger.putProperty(TRACE_ID_PROPERTY, traceId));
    }

    private static String defaultNameSpace() {
        MetricsContext context = MetricsLoggerHelper.metricsContext();
        return "aws-embedded-metrics".equals(context.getNamespace()) ?
                SystemWrapper.getenv("POWERTOOLS_METRICS_NAMESPACE"): context.getNamespace();
    }

    private static Optional<String> awsRequestId() {
        MetricsContext context = MetricsLoggerHelper.metricsContext();
        return ofNullable(context.getProperty(REQUEST_ID_PROPERTY))
                .map(Object::toString);
    }

    private static MetricsLogger logger() {
        MetricsContext metricsContext = new MetricsContext();

        if (hasDefaultDimension()) {
            metricsContext.setDefaultDimensions(new DimensionSet());
            metricsContext.setDimensions(defaultDimensions);
        }

        return new MetricsLogger(new EnvironmentProvider(), metricsContext);
    }
}
