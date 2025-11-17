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

import software.amazon.lambda.powertools.common.internal.LambdaConstants;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.internal.RequestScopedMetricsProxy;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;
import software.amazon.lambda.powertools.metrics.testutils.TestMetricsProvider;

class MetricsFactoryTest {

    private static final String TEST_NAMESPACE = "TestNamespace";
    private static final String TEST_SERVICE = "TestService";

    private static final PrintStream STANDARD_OUT = System.out;
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
        System.setOut(STANDARD_OUT);
        System.clearProperty(LambdaConstants.XRAY_TRACE_HEADER);

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
                .isInstanceOf(RequestScopedMetricsProxy.class);

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
    void shouldIsolateMetricsByTraceId() throws Exception {
        // GIVEN
        Metrics metrics = MetricsFactory.getMetricsInstance();

        // WHEN - Simulate Lambda invocation 1 with trace ID 1
        System.setProperty(LambdaConstants.XRAY_TRACE_HEADER, "Root=1-trace-id-1");
        metrics.setNamespace("TestNamespace");
        metrics.addDimension("userId", "user123");
        metrics.addMetric("ProcessedOrder", 1, MetricUnit.COUNT);
        metrics.flush();

        // WHEN - Simulate Lambda invocation 2 with trace ID 2
        System.setProperty(LambdaConstants.XRAY_TRACE_HEADER, "Root=1-trace-id-2");
        metrics.setNamespace("TestNamespace");
        metrics.addDimension("userId", "user456");
        metrics.addMetric("ProcessedOrder", 1, MetricUnit.COUNT);
        metrics.flush();

        // THEN - Verify each invocation has isolated metrics
        String emfOutput = outputStreamCaptor.toString().trim();
        String[] jsonLines = emfOutput.split("\n");
        assertThat(jsonLines).hasSize(2);

        JsonNode output1 = objectMapper.readTree(jsonLines[0]);
        JsonNode output2 = objectMapper.readTree(jsonLines[1]);

        assertThat(output1.get("userId").asText()).isEqualTo("user123");
        assertThat(output2.get("userId").asText()).isEqualTo("user456");
    }

    @Test
    void shouldUseDefaultKeyWhenNoTraceId() throws Exception {
        // GIVEN - No trace ID set
        System.clearProperty(LambdaConstants.XRAY_TRACE_HEADER);

        // WHEN
        Metrics metrics = MetricsFactory.getMetricsInstance();
        metrics.setNamespace("TestNamespace");
        metrics.addMetric("TestMetric", 1, MetricUnit.COUNT);
        metrics.flush();

        // THEN - Should work without X-Ray trace ID
        String emfOutput = outputStreamCaptor.toString().trim();
        assertThat(emfOutput).isNotEmpty();

        JsonNode rootNode = objectMapper.readTree(emfOutput);
        assertThat(rootNode.get("TestMetric").asDouble()).isEqualTo(1.0);
    }
}
