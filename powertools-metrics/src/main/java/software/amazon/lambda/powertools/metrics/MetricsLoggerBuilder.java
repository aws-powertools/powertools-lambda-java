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
 * Builder for configuring the singleton MetricsLogger instance
 */
public class MetricsLoggerBuilder {
    private MetricsProvider provider;
    private String namespace;
    private String service;
    private boolean raiseOnEmptyMetrics = false;
    private final Map<String, String> defaultDimensions = new LinkedHashMap<>();

    private MetricsLoggerBuilder() {
    }

    /**
     * Create a new builder instance
     *
     * @return a new builder instance
     */
    public static MetricsLoggerBuilder builder() {
        return new MetricsLoggerBuilder();
    }

    /**
     * Set the metrics provider
     *
     * @param provider the metrics provider
     * @return this builder
     */
    public MetricsLoggerBuilder withMetricsProvider(MetricsProvider provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Set the namespace
     *
     * @param namespace the namespace
     * @return this builder
     */
    public MetricsLoggerBuilder withNamespace(String namespace) {
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
    public MetricsLoggerBuilder withService(String service) {
        this.service = service;
        return this;
    }

    /**
     * Set whether to raise an exception if no metrics are emitted
     *
     * @param raiseOnEmptyMetrics true to raise an exception, false otherwise
     * @return this builder
     */
    public MetricsLoggerBuilder withRaiseOnEmptyMetrics(boolean raiseOnEmptyMetrics) {
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
    public MetricsLoggerBuilder withDefaultDimension(String key, String value) {
        this.defaultDimensions.put(key, value);
        return this;
    }

    /**
     * Add default dimensions
     *
     * @param dimensionSet the dimension set to add
     * @return this builder
     */
    public MetricsLoggerBuilder withDefaultDimensions(DimensionSet dimensionSet) {
        if (dimensionSet != null) {
            this.defaultDimensions.putAll(dimensionSet.getDimensions());
        }
        return this;
    }

    /**
     * Configure and return the singleton MetricsLogger instance
     *
     * @return the configured singleton MetricsLogger instance
     */
    public MetricsLogger build() {
        if (provider != null) {
            MetricsLoggerFactory.setMetricsProvider(provider);
        }

        MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        if (namespace != null) {
            metricsLogger.setNamespace(namespace);
        }

        metricsLogger.setRaiseOnEmptyMetrics(raiseOnEmptyMetrics);

        if (service != null) {
            metricsLogger.setDefaultDimensions(DimensionSet.of("Service", service));
        }

        // If the user provided default dimension, we overwrite the default Service dimension again
        if (!defaultDimensions.isEmpty()) {
            metricsLogger.setDefaultDimensions(DimensionSet.of(defaultDimensions));
        }

        return metricsLogger;
    }
}
