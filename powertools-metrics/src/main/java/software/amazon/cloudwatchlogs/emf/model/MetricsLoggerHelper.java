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

package software.amazon.cloudwatchlogs.emf.model;


import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

import java.lang.reflect.Field;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.lambda.powertools.metrics.exception.InvalidMetricDimensionException;

public final class MetricsLoggerHelper {

    private MetricsLoggerHelper() {
    }

    public static boolean hasNoMetrics() {
        return metricsContext().getRootNode().getAws().isEmpty();
    }

    public static long dimensionsCount() {
        try {
            return metricsContext().getDimensions().size();
        } catch (DimensionSetExceededException e) {
            throw new InvalidMetricDimensionException(e);
        }
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
