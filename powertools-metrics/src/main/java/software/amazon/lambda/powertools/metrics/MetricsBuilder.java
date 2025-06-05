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

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;

import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;

/**
 * Builder for configuring the singleton Metrics instance
 */
public class MetricsBuilder {
    private MetricsProvider provider;
    private String namespace;
    private String service;
    private boolean raiseOnEmptyMetrics = false;
    private final Map<String, String> defaultDimensions = new LinkedHashMap<>();

    private MetricsBuilder() {
    }

    /**
     * Create a new builder instance
     *
     * @return a new builder instance
     */
    public static MetricsBuilder builder() {
        return new MetricsBuilder();
    }

    /**
     * Set the metrics provider
     *
     * @param provider the metrics provider
     * @return this builder
     */
    public MetricsBuilder withMetricsProvider(MetricsProvider provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Set the namespace
     *
     * @param namespace the namespace
     * @return this builder
     */
    public MetricsBuilder withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Set the service name. Does not apply if used in combination with default dimensions. If you would like to use a
     * service name with default dimensions, use {@link #withDefaultDimension(String, String)} instead.
     *
     * @param service the service name
     * @return this builder
     */
    public MetricsBuilder withService(String service) {
        this.service = service;
        return this;
    }

    /**
     * Set whether to raise an exception if no metrics are emitted
     *
     * @param raiseOnEmptyMetrics true to raise an exception, false otherwise
     * @return this builder
     */
    public MetricsBuilder withRaiseOnEmptyMetrics(boolean raiseOnEmptyMetrics) {
        this.raiseOnEmptyMetrics = raiseOnEmptyMetrics;
        return this;
    }

    /**
     * Add a default dimension.
     *
     * @param key   the dimension key
     * @param value the dimension value
     * @return this builder
     */
    public MetricsBuilder withDefaultDimension(String key, String value) {
        this.defaultDimensions.put(key, value);
        return this;
    }

    /**
     * Add default dimensions
     *
     * @param dimensionSet the dimension set to add
     * @return this builder
     */
    public MetricsBuilder withDefaultDimensions(DimensionSet dimensionSet) {
        if (dimensionSet != null) {
            this.defaultDimensions.putAll(dimensionSet.getDimensions());
        }
        return this;
    }

    /**
     * Configure and return the singleton Metrics instance
     *
     * @return the configured singleton Metrics instance
     */
    public Metrics build() {
        if (provider != null) {
            MetricsFactory.setMetricsProvider(provider);
        }

        Metrics metrics = MetricsFactory.getMetricsInstance();

        if (namespace != null) {
            metrics.setNamespace(namespace);
        }

        metrics.setRaiseOnEmptyMetrics(raiseOnEmptyMetrics);

        if (service != null) {
            metrics.setDefaultDimensions(DimensionSet.of("Service", service));
        }

        // If the user provided default dimension, we overwrite the default Service dimension again
        if (!defaultDimensions.isEmpty()) {
            metrics.setDefaultDimensions(DimensionSet.of(defaultDimensions));
        }

        return metrics;
    }
}
