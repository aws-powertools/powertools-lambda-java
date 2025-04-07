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

import java.time.Clock;
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

import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;
import software.amazon.lambda.powertools.testutils.metrics.MetricsFetcher;

public class MetricsE2ET {
    private static final String namespace = "MetricsE2ENamespace_" + UUID.randomUUID();
    private static final String service = "MetricsE2EService_" + UUID.randomUUID();
    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(MetricsE2ET.class.getSimpleName())
                .pathToFunction("metrics")
                .environmentVariables(
                        Stream.of(new String[][] {
                                { "POWERTOOLS_METRICS_NAMESPACE", namespace },
                                { "POWERTOOLS_SERVICE_NAME", service }
                        })
                                .collect(Collectors.toMap(data -> data[0], data -> data[1])))
                .build();
        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
    }

    @AfterAll
    public static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @Test
    public void test_recordMetrics() {
        // GIVEN

        Instant currentTimeTruncatedToMinutes = Instant.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MINUTES);
        String event1 = "{ \"metrics\": {\"orders\": 1, \"products\": 4}, \"dimensions\": { \"Environment\": \"test\"}, \"highResolution\": \"false\"}";

        String event2 = "{ \"metrics\": {\"orders\": 1, \"products\": 8}, \"dimensions\": { \"Environment\": \"test\"}, \"highResolution\": \"true\"}";
        // WHEN
        InvocationResult invocationResult = invokeFunction(functionName, event1);

        invokeFunction(functionName, event2);

        // THEN
        MetricsFetcher metricsFetcher = new MetricsFetcher();
        List<Double> coldStart = metricsFetcher.fetchMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60,
                namespace,
                "ColdStart", Stream.of(new String[][] {
                        { "FunctionName", functionName },
                        { "Service", service } }).collect(Collectors.toMap(data -> data[0], data -> data[1])));
        assertThat(coldStart.get(0)).isEqualTo(1);
        List<Double> orderMetrics = metricsFetcher.fetchMetrics(invocationResult.getStart(), invocationResult.getEnd(),
                60, namespace,
                "orders", Collections.singletonMap("Environment", "test"));
        assertThat(orderMetrics.get(0)).isEqualTo(2);
        List<Double> productMetrics = metricsFetcher.fetchMetrics(invocationResult.getStart(),
                invocationResult.getEnd(), 60, namespace,
                "products", Collections.singletonMap("Environment", "test"));

        // When searching across a 1 minute time period with a period of 60 we find both metrics and the sum is 12

        assertThat(productMetrics.get(0)).isEqualTo(12);

        orderMetrics = metricsFetcher.fetchMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60,
                namespace,
                "orders", Collections.singletonMap("Service", service));
        assertThat(orderMetrics.get(0)).isEqualTo(2);
        productMetrics = metricsFetcher.fetchMetrics(invocationResult.getStart(), invocationResult.getEnd(), 60,
                namespace,
                "products", Collections.singletonMap("Service", service));
        assertThat(productMetrics.get(0)).isEqualTo(12);

        Instant searchStartTime = currentTimeTruncatedToMinutes.plusSeconds(15);
        Instant searchEndTime = currentTimeTruncatedToMinutes.plusSeconds(45);

        List<Double> productMetricDataResult = metricsFetcher.fetchMetrics(searchStartTime, searchEndTime, 1, namespace,
                "products", Collections.singletonMap("Environment", "test"));

        // We are searching across the time period the metric was created but with a period of 1 second. Only the high
        // resolution metric will be available at this point

        assertThat(productMetricDataResult.get(0)).isEqualTo(8);

    }
}
