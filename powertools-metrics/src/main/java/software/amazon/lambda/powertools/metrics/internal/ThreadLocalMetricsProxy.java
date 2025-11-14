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

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;

public class ThreadLocalMetricsProxy implements Metrics {
    private final InheritableThreadLocal<Metrics> threadLocalMetrics = new InheritableThreadLocal<>();
    private final MetricsProvider provider;
    private final AtomicReference<String> initialNamespace = new AtomicReference<>();
    private final AtomicReference<DimensionSet> initialDefaultDimensions = new AtomicReference<>();
    private final AtomicBoolean initialRaiseOnEmptyMetrics = new AtomicBoolean(false);

    public ThreadLocalMetricsProxy(MetricsProvider provider) {
        this.provider = provider;
    }

    private Metrics getOrCreateThreadLocalMetrics() {
        Metrics metrics = threadLocalMetrics.get();
        if (metrics == null) {
            metrics = provider.getMetricsInstance();
            String namespace = initialNamespace.get();
            if (namespace != null) {
                metrics.setNamespace(namespace);
            }
            DimensionSet dimensions = initialDefaultDimensions.get();
            if (dimensions != null) {
                metrics.setDefaultDimensions(dimensions);
            }
            metrics.setRaiseOnEmptyMetrics(initialRaiseOnEmptyMetrics.get());
            threadLocalMetrics.set(metrics);
        }
        return metrics;
    }

    // Configuration methods - called by MetricsFactory and MetricsBuilder
    // These methods DO NOT eagerly create thread-local instances because they are typically called
    // outside the Lambda handler (e.g., during class initialization) potentially on a different thread.
    // We delay instance creation until the first operation that needs the metrics backend (e.g., addMetric).
    // See {@link software.amazon.lambda.powertools.metrics.MetricsFactory#getMetricsInstance()}
    // and {@link software.amazon.lambda.powertools.metrics.MetricsBuilder#build()}

    @Override
    public void setNamespace(String namespace) {
        this.initialNamespace.set(namespace);
        Optional.ofNullable(threadLocalMetrics.get()).ifPresent(m -> m.setNamespace(namespace));
    }

    @Override
    public void setDefaultDimensions(DimensionSet dimensionSet) {
        if (dimensionSet == null) {
            throw new IllegalArgumentException("DimensionSet cannot be null");
        }
        this.initialDefaultDimensions.set(dimensionSet);
        Optional.ofNullable(threadLocalMetrics.get()).ifPresent(m -> m.setDefaultDimensions(dimensionSet));
    }

    @Override
    public void setRaiseOnEmptyMetrics(boolean raiseOnEmptyMetrics) {
        this.initialRaiseOnEmptyMetrics.set(raiseOnEmptyMetrics);
        Optional.ofNullable(threadLocalMetrics.get()).ifPresent(m -> m.setRaiseOnEmptyMetrics(raiseOnEmptyMetrics));
    }

    @Override
    public DimensionSet getDefaultDimensions() {
        Metrics metrics = threadLocalMetrics.get();
        if (metrics != null) {
            return metrics.getDefaultDimensions();
        }
        DimensionSet dimensions = initialDefaultDimensions.get();
        return dimensions != null ? dimensions : DimensionSet.of(new HashMap<>());
    }

    // Metrics operations - these eagerly create thread-local instances

    @Override
    public void addMetric(String key, double value, MetricUnit unit, MetricResolution resolution) {
        getOrCreateThreadLocalMetrics().addMetric(key, value, unit, resolution);
    }

    @Override
    public void addDimension(DimensionSet dimensionSet) {
        getOrCreateThreadLocalMetrics().addDimension(dimensionSet);
    }

    @Override
    public void setTimestamp(Instant timestamp) {
        getOrCreateThreadLocalMetrics().setTimestamp(timestamp);
    }

    @Override
    public void addMetadata(String key, Object value) {
        getOrCreateThreadLocalMetrics().addMetadata(key, value);
    }

    @Override
    public void clearDefaultDimensions() {
        getOrCreateThreadLocalMetrics().clearDefaultDimensions();
    }

    @Override
    public void flush() {
        // Always create instance to ensure validation and warnings are triggered. E.g. when raiseOnEmptyMetrics
        // is enabled.
        Metrics metrics = getOrCreateThreadLocalMetrics();
        metrics.flush();
        threadLocalMetrics.remove();
    }

    @Override
    public void captureColdStartMetric(Context context, DimensionSet dimensions) {
        getOrCreateThreadLocalMetrics().captureColdStartMetric(context, dimensions);
    }

    @Override
    public void captureColdStartMetric(DimensionSet dimensions) {
        getOrCreateThreadLocalMetrics().captureColdStartMetric(dimensions);
    }

    @Override
    public void flushMetrics(Consumer<Metrics> metricsConsumer) {
        getOrCreateThreadLocalMetrics().flushMetrics(metricsConsumer);
    }
}
