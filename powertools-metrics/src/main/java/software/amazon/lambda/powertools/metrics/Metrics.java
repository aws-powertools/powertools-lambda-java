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

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

/**
 * Interface for metrics implementations.
 * This interface is used to collect metrics in the Lambda function.
 * It provides methods to add metrics, dimensions, and metadata.
 */
public interface Metrics {

    /**
     * Add a metric
     *
     * @param key        the name of the metric
     * @param value      the value of the metric
     * @param unit       the unit of the metric
     * @param resolution the resolution of the metric
     */
    void addMetric(String key, double value, MetricUnit unit, MetricResolution resolution);

    /**
     * Add a metric with default resolution
     *
     * @param key   the name of the metric
     * @param value the value of the metric
     * @param unit  the unit of the metric
     */
    default void addMetric(String key, double value, MetricUnit unit) {
        addMetric(key, value, unit, MetricResolution.STANDARD);
    }

    /**
     * Add a metric with default unit and resolution
     *
     * @param key   the name of the metric
     * @param value the value of the metric
     */
    default void addMetric(String key, double value) {
        addMetric(key, value, MetricUnit.NONE, MetricResolution.STANDARD);
    }

    /**
     * Add a dimension
     * This is equivalent to calling {@code addDimension(DimensionSet.of(key, value))}
     *
     * @param key   the name of the dimension
     * @param value the value of the dimension
     */
    default void addDimension(String key, String value) {
        addDimension(DimensionSet.of(key, value));
    }

    /**
     * Add a dimension set
     *
     * @param dimensionSet the dimension set to add
     */
    void addDimension(DimensionSet dimensionSet);

    /**
     * Add metadata
     *
     * @param key   the name of the metadata
     * @param value the value of the metadata
     */
    void addMetadata(String key, Object value);

    /**
     * Set default dimensions
     *
     * @param dimensionSet the dimension set to use as default dimensions
     */
    void setDefaultDimensions(DimensionSet dimensionSet);

    /**
     * Get the default dimensions
     *
     * @return the default dimensions as a DimensionSet
     */
    DimensionSet getDefaultDimensions();

    /**
     * Set the namespace
     *
     * @param namespace the namespace
     */
    void setNamespace(String namespace);

    /**
     * Set whether to raise an exception if no metrics are emitted
     *
     * @param raiseOnEmptyMetrics true to raise an exception, false otherwise
     */
    void setRaiseOnEmptyMetrics(boolean raiseOnEmptyMetrics);

    /**
     * Clear default dimensions
     */
    void clearDefaultDimensions();

    /**
     * Flush metrics to the configured sink
     */
    void flush();

    /**
     * Capture cold start metric and flush immediately
     *
     * @param context Lambda context
     * @param dimensions custom dimensions for this metric (optional)
     */
    void captureColdStartMetric(Context context, DimensionSet dimensions);

    /**
     * Capture cold start metric and flush immediately
     *
     * @param context Lambda context
     */
    default void captureColdStartMetric(Context context) {
        captureColdStartMetric(context, null);
    }

    /**
     * Capture cold start metric without Lambda context and flush immediately
     *
     * @param dimensions custom dimensions for this metric (optional)
     */
    void captureColdStartMetric(DimensionSet dimensions);

    /**
     * Capture cold start metric without Lambda context and flush immediately
     */
    default void captureColdStartMetric() {
        captureColdStartMetric((DimensionSet) null);
    }

    /**
     * Flush a single metric with custom dimensions. This creates a separate metrics context
     * that doesn't affect the default metrics context.
     *
     * @param name       the name of the metric
     * @param value      the value of the metric
     * @param unit       the unit of the metric
     * @param namespace  the namespace for the metric
     * @param dimensions custom dimensions for this metric (optional)
     */
    void flushSingleMetric(String name, double value, MetricUnit unit, String namespace, DimensionSet dimensions);

    /**
     * Flush a single metric with custom dimensions. This creates a separate metrics context
     * that doesn't affect the default metrics context.
     *
     * @param name       the name of the metric
     * @param value      the value of the metric
     * @param unit       the unit of the metric
     * @param namespace  the namespace for the metric
     */
    default void flushSingleMetric(String name, double value, MetricUnit unit, String namespace) {
        flushSingleMetric(name, value, unit, namespace, null);
    }
}
