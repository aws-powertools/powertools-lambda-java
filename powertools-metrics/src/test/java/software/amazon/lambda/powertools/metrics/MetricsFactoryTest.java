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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.internal.ThreadLocalMetricsProxy;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;
import software.amazon.lambda.powertools.metrics.testutils.TestMetricsProvider;

class MetricsFactoryTest {

    private static final String TEST_NAMESPACE = "TestNamespace";
    private static final String TEST_SERVICE = "TestService";

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        System.setOut(new PrintStream(outputStreamCaptor));

        // Reset LambdaHandlerProcessor's serviceName
        Method resetServiceName = LambdaHandlerProcessor.class.getDeclaredMethod("resetServiceName");
        resetServiceName.setAccessible(true);
        resetServiceName.invoke(null);

        // Reset isColdStart
        java.lang.reflect.Field coldStartField = LambdaHandlerProcessor.class.getDeclaredField("isColdStart");
        coldStartField.setAccessible(true);
        coldStartField.set(null, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(standardOut);

        // Reset the singleton state between tests
        java.lang.reflect.Field field = MetricsFactory.class.getDeclaredField("metricsProxy");
        field.setAccessible(true);
        field.set(null, null);

        field = MetricsFactory.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(null, new software.amazon.lambda.powertools.metrics.provider.EmfMetricsProvider());
    }

    @Test
    void shouldGetMetricsInstance() {
        // When
        Metrics metrics = MetricsFactory.getMetricsInstance();

        // Then
        assertThat(metrics).isNotNull();
    }

    @Test
    void shouldReturnSameInstanceOnMultipleCalls() {
        // When
        Metrics firstInstance = MetricsFactory.getMetricsInstance();
        Metrics secondInstance = MetricsFactory.getMetricsInstance();

        // Then
        assertThat(firstInstance).isSameAs(secondInstance);
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = TEST_NAMESPACE)
    void shouldUseNamespaceFromEnvironmentVariable() throws Exception {
        // When
        Metrics metrics = MetricsFactory.getMetricsInstance();
        metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo(TEST_NAMESPACE);
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = TEST_SERVICE)
    void shouldUseServiceNameFromEnvironmentVariable() throws Exception {
        // When
        Metrics metrics = MetricsFactory.getMetricsInstance();
        metrics.setNamespace("TestNamespace");
        metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo(TEST_SERVICE);
    }

    @Test
    void shouldSetCustomMetricsProvider() throws Exception {
        // Given
        MetricsProvider testProvider = new TestMetricsProvider();

        // When
        MetricsFactory.setMetricsProvider(testProvider);
        Metrics metrics = MetricsFactory.getMetricsInstance();

        // Then
        assertThat(metrics)
                .isInstanceOf(ThreadLocalMetricsProxy.class);

        java.lang.reflect.Field providerField = metrics.getClass().getDeclaredField("provider");
        providerField.setAccessible(true);
        MetricsProvider actualProvider = (MetricsProvider) providerField.get(metrics);
        assertThat(actualProvider).isSameAs(testProvider);
    }

    @Test
    void shouldThrowExceptionWhenSettingNullProvider() {
        // When/Then
        assertThatThrownBy(() -> MetricsFactory.setMetricsProvider(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metrics provider cannot be null");
    }

    @Test
    void shouldNotSetServiceDimensionWhenServiceUndefined() throws Exception {
        // Given - no POWERTOOLS_SERVICE_NAME set, so it will use the default undefined value

        // When
        Metrics metrics = MetricsFactory.getMetricsInstance();
        metrics.setNamespace("TestNamespace");
        metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // Service dimension should not be present
        assertThat(rootNode.has("Service")).isFalse();
    }

    @Test
    void concurrentInvocations_shouldIsolateDimensions() throws Exception {
        // GIVEN - Simulate real Lambda scenario: Metrics instance created outside handler
        Metrics metrics = MetricsFactory.getMetricsInstance();

        CountDownLatch latch = new CountDownLatch(2);
        Exception[] exceptions = new Exception[2];

        Thread thread1 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();

                // Simulate handleRequest execution
                metrics.setNamespace("TestNamespace");
                metrics.addDimension("userId", "user123");
                metrics.addMetric("ProcessedOrder", 1, MetricUnit.COUNT);
                metrics.flush();
            } catch (Exception e) {
                exceptions[0] = e;
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await();

                // Simulate handleRequest execution
                metrics.setNamespace("TestNamespace");
                metrics.addDimension("userId", "user456");
                metrics.addMetric("ProcessedOrder", 1, MetricUnit.COUNT);
                metrics.flush();
            } catch (Exception e) {
                exceptions[1] = e;
            }
        });

        // WHEN
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // THEN
        assertThat(exceptions[0]).isNull();
        assertThat(exceptions[1]).isNull();

        String emfOutput = outputStreamCaptor.toString().trim();
        String[] jsonLines = emfOutput.split("\n");
        assertThat(jsonLines).hasSize(2);

        JsonNode output1 = objectMapper.readTree(jsonLines[0]);
        JsonNode output2 = objectMapper.readTree(jsonLines[1]);

        // Check if dimensions are leaking across threads
        boolean hasLeakage = false;

        if (output1.has("userId") && output2.has("userId")) {
            String userId1 = output1.get("userId").asText();
            String userId2 = output2.get("userId").asText();
            // Both should have different userIds
            hasLeakage = userId1.equals(userId2);
        }

        // Each thread should have isolated dimensions
        assertThat(hasLeakage)
                .as("Dimensions should NOT leak across threads - each thread should have its own userId")
                .isFalse();
    }
}
