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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;
import software.amazon.lambda.powertools.metrics.testutils.TestMetricsLogger;
import software.amazon.lambda.powertools.metrics.testutils.TestMetricsProvider;

class MetricsLoggerBuilderTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(standardOut);

        // Reset the singleton state between tests
        java.lang.reflect.Field field = MetricsLoggerFactory.class.getDeclaredField("metricsLogger");
        field.setAccessible(true);
        field.set(null, null);

        field = MetricsLoggerFactory.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(null, new software.amazon.lambda.powertools.metrics.provider.EmfMetricsProvider());
    }

    @Test
    void shouldBuildWithCustomNamespace() throws Exception {
        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withNamespace("CustomNamespace")
                .build();

        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("CustomNamespace");
    }

    @Test
    void shouldBuildWithCustomService() throws Exception {
        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withService("CustomService")
                .build();

        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("CustomService");
    }

    @Test
    void shouldBuildWithRaiseOnEmptyMetrics() {
        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withRaiseOnEmptyMetrics(true)
                .build();

        // Then
        assertThat(metricsLogger).isNotNull();
        assertThatThrownBy(metricsLogger::flush)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No metrics were emitted");
    }

    @Test
    void shouldBuildWithDefaultDimension() throws Exception {
        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withDefaultDimension("Environment", "Test")
                .build();

        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Environment")).isTrue();
        assertThat(rootNode.get("Environment").asText()).isEqualTo("Test");
    }

    @Test
    void shouldBuildWithMultipleDefaultDimensions() throws Exception {
        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withDefaultDimensions(Map.of("Environment", "Test", "Region", "us-west-2"))
                .build();

        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Environment")).isTrue();
        assertThat(rootNode.get("Environment").asText()).isEqualTo("Test");
        assertThat(rootNode.has("Region")).isTrue();
        assertThat(rootNode.get("Region").asText()).isEqualTo("us-west-2");
    }

    @Test
    void shouldBuildWithCustomMetricsProvider() {
        // Given
        MetricsProvider testProvider = new TestMetricsProvider();

        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withMetricsProvider(testProvider)
                .build();

        // Then
        assertThat(metricsLogger).isInstanceOf(TestMetricsLogger.class);
    }

    @Test
    void shouldOverrideServiceWithDefaultDimensions() throws Exception {
        // When
        MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
                .withService("OriginalService")
                .withDefaultDimensions(Map.of("Service", "OverriddenService"))
                .build();

        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("OverriddenService");
    }
}
