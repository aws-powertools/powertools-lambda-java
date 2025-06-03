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

package software.amazon.lambda.powertools.metrics.internal;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

/**
 * Implementation of MetricsLogger that uses the EMF library. Proxies MetricsLogger interface calls to underlying
 * library {@link software.amazon.cloudwatchlogs.emf.logger.MetricsLogger}.
 */
public class EmfMetricsLogger implements MetricsLogger {
    private static final String TRACE_ID_PROPERTY = "xray_trace_id";
    private static final String REQUEST_ID_PROPERTY = "function_request_id";
    private static final String COLD_START_METRIC = "ColdStart";

    private final software.amazon.cloudwatchlogs.emf.logger.MetricsLogger emfLogger;
    private final EnvironmentProvider environmentProvider;
    private boolean raiseOnEmptyMetrics = false;
    private String namespace;
    private Map<String, String> defaultDimensions = new HashMap<>();
    private final AtomicBoolean hasMetrics = new AtomicBoolean(false);

    public EmfMetricsLogger(EnvironmentProvider environmentProvider, MetricsContext metricsContext) {
        this.emfLogger = new software.amazon.cloudwatchlogs.emf.logger.MetricsLogger(environmentProvider,
                metricsContext);
        this.environmentProvider = environmentProvider;
    }

    @Override
    public void addMetric(String key, double value, MetricUnit unit, MetricResolution resolution) {
        StorageResolution storageResolution = resolution == MetricResolution.HIGH ? StorageResolution.HIGH
                : StorageResolution.STANDARD;
        emfLogger.putMetric(key, value, convertUnit(unit), storageResolution);
        hasMetrics.set(true);
    }

    @Override
    public void addDimension(String key, String value) {
        DimensionSet dimensionSet = new DimensionSet();
        try {
            dimensionSet.addDimension(key, value);
            emfLogger.putDimensions(dimensionSet);
            // Update our local copy of default dimensions
            defaultDimensions.put(key, value);
        } catch (Exception e) {
            // Ignore dimension errors
        }
    }

    @Override
    public void addMetadata(String key, Object value) {
        emfLogger.putMetadata(key, value);
    }

    @Override
    public void setDefaultDimensions(Map<String, String> defaultDimensions) {
        DimensionSet dimensionSet = new DimensionSet();
        defaultDimensions.forEach((key, value) -> {
            try {
                dimensionSet.addDimension(key, value);
            } catch (Exception e) {
                // Ignore dimension errors
            }
        });
        emfLogger.setDimensions(dimensionSet);
        // Store a copy of the default dimensions
        this.defaultDimensions = new LinkedHashMap<>(defaultDimensions);
    }

    @Override
    public software.amazon.lambda.powertools.metrics.model.DimensionSet getDefaultDimensions() {
        return software.amazon.lambda.powertools.metrics.model.DimensionSet.of(defaultDimensions);
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
        try {
            emfLogger.setNamespace(namespace);
        } catch (Exception e) {
            // Ignore namespace errors
        }
    }

    @Override
    public void setRaiseOnEmptyMetrics(boolean raiseOnEmptyMetrics) {
        this.raiseOnEmptyMetrics = raiseOnEmptyMetrics;
    }

    @Override
    public void clearDefaultDimensions() {
        emfLogger.resetDimensions(false);
        defaultDimensions.clear();
    }

    @Override
    public void flush() {
        if (raiseOnEmptyMetrics && !hasMetrics.get()) {
            throw new IllegalStateException("No metrics were emitted");
        }
        emfLogger.flush();
    }

    @Override
    public void captureColdStartMetric(Context context,
            software.amazon.lambda.powertools.metrics.model.DimensionSet dimensions) {
        if (isColdStart()) {
            software.amazon.cloudwatchlogs.emf.logger.MetricsLogger coldStartLogger = new software.amazon.cloudwatchlogs.emf.logger.MetricsLogger();

            // Set namespace if available
            if (namespace != null) {
                try {
                    coldStartLogger.setNamespace(namespace);
                } catch (Exception e) {
                    // Ignore namespace errors
                }
            }

            coldStartLogger.putMetric(COLD_START_METRIC, 1, Unit.COUNT);

            // Set dimensions if provided
            if (dimensions != null) {
                DimensionSet emfDimensionSet = new DimensionSet();
                dimensions.getDimensions().forEach((key, val) -> {
                    try {
                        emfDimensionSet.addDimension(key, val);
                    } catch (Exception e) {
                        // Ignore dimension errors
                    }
                });
                coldStartLogger.setDimensions(emfDimensionSet);
            }

            // Add request ID from context if available
            if (context != null) {
                coldStartLogger.putProperty(REQUEST_ID_PROPERTY, context.getAwsRequestId());
            }

            // Add trace ID using the standard logic
            getXrayTraceId().ifPresent(traceId -> coldStartLogger.putProperty(TRACE_ID_PROPERTY, traceId));

            coldStartLogger.flush();
        }
    }

    @Override
    public void captureColdStartMetric(software.amazon.lambda.powertools.metrics.model.DimensionSet dimensions) {
        captureColdStartMetric(null, dimensions);
    }

    @Override
    public void pushSingleMetric(String name, double value, MetricUnit unit, String namespace,
            software.amazon.lambda.powertools.metrics.model.DimensionSet dimensions) {
        // Create a new logger for this single metric
        software.amazon.cloudwatchlogs.emf.logger.MetricsLogger singleMetricLogger = new software.amazon.cloudwatchlogs.emf.logger.MetricsLogger(
                environmentProvider);

        // Set namespace (now mandatory)
        try {
            singleMetricLogger.setNamespace(namespace);
        } catch (Exception e) {
            // Ignore namespace errors
        }

        // Add the metric
        singleMetricLogger.putMetric(name, value, convertUnit(unit));

        // Set dimensions if provided
        if (dimensions != null) {
            DimensionSet emfDimensionSet = new DimensionSet();
            dimensions.getDimensions().forEach((key, val) -> {
                try {
                    emfDimensionSet.addDimension(key, val);
                } catch (Exception e) {
                    // Ignore dimension errors
                }
            });
            singleMetricLogger.setDimensions(emfDimensionSet);
        }

        // Flush the metric
        singleMetricLogger.flush();
    }

    private Unit convertUnit(MetricUnit unit) {
        switch (unit) {
            case SECONDS:
                return Unit.SECONDS;
            case MICROSECONDS:
                return Unit.MICROSECONDS;
            case MILLISECONDS:
                return Unit.MILLISECONDS;
            case BYTES:
                return Unit.BYTES;
            case KILOBYTES:
                return Unit.KILOBYTES;
            case MEGABYTES:
                return Unit.MEGABYTES;
            case GIGABYTES:
                return Unit.GIGABYTES;
            case TERABYTES:
                return Unit.TERABYTES;
            case BITS:
                return Unit.BITS;
            case KILOBITS:
                return Unit.KILOBITS;
            case MEGABITS:
                return Unit.MEGABITS;
            case GIGABITS:
                return Unit.GIGABITS;
            case TERABITS:
                return Unit.TERABITS;
            case PERCENT:
                return Unit.PERCENT;
            case COUNT:
                return Unit.COUNT;
            case BYTES_SECOND:
                return Unit.BYTES_SECOND;
            case KILOBYTES_SECOND:
                return Unit.KILOBYTES_SECOND;
            case MEGABYTES_SECOND:
                return Unit.MEGABYTES_SECOND;
            case GIGABYTES_SECOND:
                return Unit.GIGABYTES_SECOND;
            case TERABYTES_SECOND:
                return Unit.TERABYTES_SECOND;
            case BITS_SECOND:
                return Unit.BITS_SECOND;
            case KILOBITS_SECOND:
                return Unit.KILOBITS_SECOND;
            case MEGABITS_SECOND:
                return Unit.MEGABITS_SECOND;
            case GIGABITS_SECOND:
                return Unit.GIGABITS_SECOND;
            case TERABITS_SECOND:
                return Unit.TERABITS_SECOND;
            case COUNT_SECOND:
                return Unit.COUNT_SECOND;
            case NONE:
            default:
                return Unit.NONE;
        }
    }
}
