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

package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.Instant;


public class Function implements RequestHandler<Input, String> {

    MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    @Metrics(captureColdStart = true)
    public String handleRequest(Input input, Context context) {

        Instant currentTimeTruncatedPlusThirty =
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toInstant(ZoneOffset.UTC).plusSeconds(30);
        metricsLogger.setTimestamp(currentTimeTruncatedPlusThirty);

        DimensionSet dimensionSet = new DimensionSet();
        input.getDimensions().forEach((key, value) -> dimensionSet.addDimension(key, value));
        metricsLogger.putDimensions(dimensionSet);

        input.getMetrics().forEach((key, value) -> metricsLogger.putMetric(key, value, Unit.COUNT,
                input.getHighResolution().equalsIgnoreCase("true") ? StorageResolution.HIGH :
                        StorageResolution.STANDARD));

        return "OK";
    }
}