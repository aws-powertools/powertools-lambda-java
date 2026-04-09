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

package software.amazon.lambda.powertools;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.testutils.Infrastructure.FUNCTION_NAME_OUTPUT;
import static software.amazon.lambda.powertools.testutils.lambda.LambdaInvoker.invokeFunction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import software.amazon.lambda.powertools.testutils.DataNotReadyException;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.RetryUtils;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;
import software.amazon.lambda.powertools.testutils.metrics.MetricsFetcher;

class MetricsE2ET {
    private static final String NAMESPACE = "MetricsE2ENamespace_" + UUID.randomUUID();
    private static final String SERVICE = "MetricsE2EService_" + UUID.randomUUID();
    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(MetricsE2ET.class.getSimpleName())
                .pathToFunction("metrics")
                .environmentVariables(
                        Stream.of(new String[][] {
                                { "POWERTOOLS_METRICS_NAMESPACE", NAMESPACE },
                                { "POWERTOOLS_SERVICE_NAME", SERVICE }
                        })
                                .collect(Collectors.toMap(data -> data[0], data -> data[1])))
                .build();
        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
    }

    @AfterAll
    static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @Test
    void test_recordMetrics() {
        // GIVEN

        String event1 = "{ \"metrics\": {\"orders\": 1, \"products\": 4}, \"dimensions\": { \"Environment\": \"test\"}, \"highResolution\": \"false\"}";

        String event2 = "{ \"metrics\": {\"orders\": 1, \"products\": 8}, \"dimensions\": { \"Environment\": \"test\"}, \"highResolution\": \"true\"}";
        // WHEN
        InvocationResult invocationResult = invokeFunction(functionName, event1);

        invokeFunction(functionName, event2);

        // THEN
        // Pad the query window to address CloudWatch eventual consistency:
        // metric timestamps can shift by up to a minute during batch processing.
        Instant paddedStart = invocationResult.getStart().minus(1, ChronoUnit.MINUTES);
        Instant paddedEnd = invocationResult.getEnd().plus(2, ChronoUnit.MINUTES);

        MetricsFetcher metricsFetcher = new MetricsFetcher();
        List<Double> coldStart = metricsFetcher.fetchMetrics(paddedStart, paddedEnd, 60,
                NAMESPACE,
                "ColdStart", Stream.of(new String[][] {
                        { "FunctionName", functionName },
                        { "Service", SERVICE } }).collect(Collectors.toMap(data -> data[0], data -> data[1])));
        assertThat(coldStart.get(0)).isEqualTo(1);
        List<Double> orderMetrics = RetryUtils.withRetry(() -> {
            List<Double> metrics = metricsFetcher.fetchMetrics(paddedStart, paddedEnd,
                    60, NAMESPACE, "orders", Collections.singletonMap("Environment", "test"));
            if (metrics.get(0) != 2.0) {
                throw new DataNotReadyException("Expected 2.0 orders but got " + metrics.get(0));
            }
            return metrics;
        }, "orderMetricsRetry", DataNotReadyException.class).get();
        assertThat(orderMetrics.get(0)).isEqualTo(2);
        List<Double> productMetrics = metricsFetcher.fetchMetrics(paddedStart,
                paddedEnd, 60, NAMESPACE,
                "products", Collections.singletonMap("Environment", "test"));

        // When searching across a 1 minute time period with a period of 60 we find both metrics and the sum is 12
        assertThat(productMetrics.get(0)).isEqualTo(12);

        orderMetrics = metricsFetcher.fetchMetrics(paddedStart, paddedEnd, 60,
                NAMESPACE,
                "orders", Collections.singletonMap("Service", SERVICE));
        assertThat(orderMetrics.get(0)).isEqualTo(2);
        productMetrics = metricsFetcher.fetchMetrics(paddedStart, paddedEnd, 60,
                NAMESPACE,
                "products", Collections.singletonMap("Service", SERVICE));
        assertThat(productMetrics.get(0)).isEqualTo(12);

        List<Double> productMetricDataResult = metricsFetcher.fetchMetrics(paddedStart,
                paddedEnd, 1, NAMESPACE,
                "products", Collections.singletonMap("Environment", "test"));

        // With a period of 1 second and a padded window, both standard (4) and high resolution (8)
        // metrics may appear as separate 1-second buckets. Verify the high resolution value is present.
        assertThat(productMetricDataResult).contains(8.0);
    }
}
