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

import software.amazon.lambda.powertools.metrics.model.DimensionSet;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.provider.EmfMetricsProvider;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;

/**
 * Factory for accessing the singleton MetricsLogger instance
 */
public final class MetricsLoggerFactory {
    private static MetricsProvider provider = new EmfMetricsProvider();
    private static MetricsLogger metricsLogger;

    private MetricsLoggerFactory() {
    }

    /**
     * Get the singleton instance of the MetricsLogger
     *
     * @return the singleton MetricsLogger instance
     */
    public static synchronized MetricsLogger getMetricsLogger() {
        if (metricsLogger == null) {
            metricsLogger = provider.getMetricsLogger();

            // Apply default configuration from environment variables
            String envNamespace = System.getenv("POWERTOOLS_METRICS_NAMESPACE");
            if (envNamespace != null) {
                metricsLogger.setNamespace(envNamespace);
            }

            metricsLogger.setDefaultDimensions(DimensionSet.of("Service", LambdaHandlerProcessor.serviceName()));
        }

        return metricsLogger;
    }

    /**
     * Set the metrics provider
     *
     * @param metricsProvider the metrics provider
     */
    public static synchronized void setMetricsProvider(MetricsProvider metricsProvider) {
        if (metricsProvider == null) {
            throw new IllegalArgumentException("Metrics provider cannot be null");
        }
        provider = metricsProvider;
        // Reset the logger so it will be recreated with the new provider
        metricsLogger = null;
    }
}
