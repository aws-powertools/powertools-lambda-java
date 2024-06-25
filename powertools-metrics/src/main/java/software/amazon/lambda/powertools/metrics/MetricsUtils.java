/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.metrics;

import static java.util.Optional.ofNullable;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.metrics.internal.LambdaMetricsAspect.REQUEST_ID_PROPERTY;
import static software.amazon.lambda.powertools.metrics.internal.LambdaMetricsAspect.TRACE_ID_PROPERTY;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.MetricsLoggerHelper;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * A class used to retrieve the instance of the {@code MetricsLogger} used by
 * {@code Metrics}.
 * <p>
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
     *
     * @param dimensionSets Default value of dimensions set for logger
     */
    public static void defaultDimensions(final DimensionSet... dimensionSets) {
        MetricsUtils.defaultDimensions = dimensionSets;
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
        withMetricsLogger(metricsLogger ->
        {
            metricsLogger.putMetric(name, value, unit);
            logger.accept(metricsLogger);
        });
    }

    /**
     * Add and immediately flush a single metric.
     * It by default captures function_request_id as property if used together with {@link Metrics} annotation. It will also
     * capture xray_trace_id as property if tracing is enabled.
     *
     * @param name      the name of the metric
     * @param value     the value of the metric
     * @param unit      the unit type of the metric
     * @param namespace the namespace associated with the metric
     * @param logger    the MetricsLogger
     */
    public static void withSingleMetric(final String name,
                                        final double value,
                                        final Unit unit,
                                        final String namespace,
                                        final Consumer<MetricsLogger> logger) {
        withMetricsLogger(metricsLogger ->
        {
            metricsLogger.setNamespace(namespace);
            metricsLogger.putMetric(name, value, unit);
            logger.accept(metricsLogger);
        });
    }

    /**
     * Provide and immediately flush a {@link MetricsLogger}. It uses the default namespace
     * specified either on {@link Metrics} annotation or via POWERTOOLS_METRICS_NAMESPACE env var.
     * It by default captures function_request_id as property if used together with {@link Metrics} annotation. It will also
     * capture xray_trace_id as property if tracing is enabled.
     *
     * @param logger the MetricsLogger
     */
    public static void withMetricsLogger(final Consumer<MetricsLogger> logger) {
        MetricsLogger metricsLogger = logger();

        try {
            metricsLogger.setNamespace(defaultNameSpace());
            captureRequestAndTraceId(metricsLogger);
            logger.accept(metricsLogger);
        } finally {
            metricsLogger.flush();
        }
    }

    public static DimensionSet[] getDefaultDimensions() {
        return Arrays.copyOf(defaultDimensions, defaultDimensions.length);
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
        if ("aws-embedded-metrics".equals(context.getNamespace())) {
            String namespace = SystemWrapper.getenv("POWERTOOLS_METRICS_NAMESPACE");
            return namespace != null ? namespace : "aws-embedded-metrics";
        } else {
            return context.getNamespace();
        }
    }

    private static Optional<String> awsRequestId() {
        MetricsContext context = MetricsLoggerHelper.metricsContext();
        return ofNullable(context.getProperty(REQUEST_ID_PROPERTY))
                .map(Object::toString);
    }

    private static MetricsLogger logger() {
        MetricsContext metricsContext = new MetricsContext();

        if (hasDefaultDimension()) {
            metricsContext.setDimensions(defaultDimensions);
        }

        return new MetricsLogger(new EnvironmentProvider(), metricsContext);
    }
}
