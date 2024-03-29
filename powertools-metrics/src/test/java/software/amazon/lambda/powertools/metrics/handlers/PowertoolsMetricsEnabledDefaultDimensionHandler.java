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

package software.amazon.lambda.powertools.metrics.handlers;

import static software.amazon.lambda.powertools.metrics.MetricsUtils.defaultDimensions;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.withSingleMetric;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;

public class PowertoolsMetricsEnabledDefaultDimensionHandler implements RequestHandler<Object, Object> {

    static {
        defaultDimensions(DimensionSet.of("CustomDimension", "booking"));
    }

    @Override
    @Metrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
        MetricsLogger metricsLogger = metricsLogger();
        metricsLogger.putMetric("Metric1", 1, Unit.BYTES);

        withSingleMetric("Metric2", 1, Unit.COUNT, log ->
        {
        });

        return null;
    }
}
