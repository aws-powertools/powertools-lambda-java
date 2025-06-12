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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;
import software.amazon.lambda.powertools.metrics.testutils.TestMetrics;
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

        // Reset LambdaHandlerProcessor's SERVICE_NAME
        Method resetServiceName = LambdaHandlerProcessor.class.getDeclaredMethod("resetServiceName");
        resetServiceName.setAccessible(true);
        resetServiceName.invoke(null);

        // Reset IS_COLD_START
        java.lang.reflect.Field coldStartField = LambdaHandlerProcessor.class.getDeclaredField("IS_COLD_START");
        coldStartField.setAccessible(true);
        coldStartField.set(null, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(standardOut);

        // Reset the singleton state between tests
        java.lang.reflect.Field field = MetricsFactory.class.getDeclaredField("metrics");
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
    void shouldSetCustomMetricsProvider() {
        // Given
        MetricsProvider testProvider = new TestMetricsProvider();

        // When
        MetricsFactory.setMetricsProvider(testProvider);
        Metrics metrics = MetricsFactory.getMetricsInstance();

        // Then
        assertThat(metrics).isInstanceOf(TestMetrics.class);
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
}
