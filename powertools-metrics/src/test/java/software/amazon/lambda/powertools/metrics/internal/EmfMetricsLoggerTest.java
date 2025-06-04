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

package software.amazon.lambda.powertools.metrics.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.testutils.TestContext;

class EmfMetricsLoggerTest {

    private MetricsLogger metricsLogger;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() throws Exception {
        // Reset LambdaHandlerProcessor's SERVICE_NAME
        Method resetServiceName = LambdaHandlerProcessor.class.getDeclaredMethod("resetServiceName");
        resetServiceName.setAccessible(true);
        resetServiceName.invoke(null);

        // Reset IS_COLD_START
        java.lang.reflect.Field coldStartField = LambdaHandlerProcessor.class.getDeclaredField("IS_COLD_START");
        coldStartField.setAccessible(true);
        coldStartField.set(null, null);

        metricsLogger = MetricsLoggerFactory.getMetricsLogger();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(standardOut);

        // Reset the singleton state between tests
        java.lang.reflect.Field field = MetricsLoggerFactory.class.getDeclaredField("metricsLogger");
        field.setAccessible(true);
        field.set(null, null);
    }

    @ParameterizedTest
    @MethodSource("unitConversionTestCases")
    void shouldConvertMetricUnits(MetricUnit inputUnit, Unit expectedUnit) throws Exception {
        // Given
        // We access using reflection here for simplicity (even though this is not best practice)
        Method convertUnitMethod = EmfMetricsLogger.class.getDeclaredMethod("convertUnit", MetricUnit.class);
        convertUnitMethod.setAccessible(true);

        // When
        Unit actualUnit = (Unit) convertUnitMethod.invoke(metricsLogger, inputUnit);

        // Then
        assertThat(actualUnit).isEqualTo(expectedUnit);
    }

    private static Stream<Arguments> unitConversionTestCases() {
        return Stream.of(
                Arguments.of(MetricUnit.SECONDS, Unit.SECONDS),
                Arguments.of(MetricUnit.MICROSECONDS, Unit.MICROSECONDS),
                Arguments.of(MetricUnit.MILLISECONDS, Unit.MILLISECONDS),
                Arguments.of(MetricUnit.BYTES, Unit.BYTES),
                Arguments.of(MetricUnit.KILOBYTES, Unit.KILOBYTES),
                Arguments.of(MetricUnit.MEGABYTES, Unit.MEGABYTES),
                Arguments.of(MetricUnit.GIGABYTES, Unit.GIGABYTES),
                Arguments.of(MetricUnit.TERABYTES, Unit.TERABYTES),
                Arguments.of(MetricUnit.BITS, Unit.BITS),
                Arguments.of(MetricUnit.KILOBITS, Unit.KILOBITS),
                Arguments.of(MetricUnit.MEGABITS, Unit.MEGABITS),
                Arguments.of(MetricUnit.GIGABITS, Unit.GIGABITS),
                Arguments.of(MetricUnit.TERABITS, Unit.TERABITS),
                Arguments.of(MetricUnit.PERCENT, Unit.PERCENT),
                Arguments.of(MetricUnit.COUNT, Unit.COUNT),
                Arguments.of(MetricUnit.BYTES_SECOND, Unit.BYTES_SECOND),
                Arguments.of(MetricUnit.KILOBYTES_SECOND, Unit.KILOBYTES_SECOND),
                Arguments.of(MetricUnit.MEGABYTES_SECOND, Unit.MEGABYTES_SECOND),
                Arguments.of(MetricUnit.GIGABYTES_SECOND, Unit.GIGABYTES_SECOND),
                Arguments.of(MetricUnit.TERABYTES_SECOND, Unit.TERABYTES_SECOND),
                Arguments.of(MetricUnit.BITS_SECOND, Unit.BITS_SECOND),
                Arguments.of(MetricUnit.KILOBITS_SECOND, Unit.KILOBITS_SECOND),
                Arguments.of(MetricUnit.MEGABITS_SECOND, Unit.MEGABITS_SECOND),
                Arguments.of(MetricUnit.GIGABITS_SECOND, Unit.GIGABITS_SECOND),
                Arguments.of(MetricUnit.TERABITS_SECOND, Unit.TERABITS_SECOND),
                Arguments.of(MetricUnit.COUNT_SECOND, Unit.COUNT_SECOND),
                Arguments.of(MetricUnit.NONE, Unit.NONE));
    }

    @Test
    void shouldCreateMetricWithDefaultResolution() throws Exception {
        // When
        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("_aws")).isTrue();
        assertThat(rootNode.get("test-metric").asDouble()).isEqualTo(100);
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Metrics").get(0).get("Unit").asText())
                .isEqualTo("Count");
    }

    @Test
    void shouldCreateMetricWithHighResolution() throws Exception {
        // When
        metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT, MetricResolution.HIGH);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("_aws")).isTrue();
        assertThat(rootNode.get("test-metric").asDouble()).isEqualTo(100);
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Metrics").get(0).get("Unit").asText())
                .isEqualTo("Count");
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Metrics").get(0).get("StorageResolution")
                .asInt()).isEqualTo(1);
    }

    @Test
    void shouldAddDimension() throws Exception {
        // When
        metricsLogger.clearDefaultDimensions(); // Clear default Service dimension first for easier assertions
        metricsLogger.addDimension("CustomDimension", "CustomValue");
        metricsLogger.addMetric("test-metric", 100);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("CustomDimension")).isTrue();
        assertThat(rootNode.get("CustomDimension").asText()).isEqualTo("CustomValue");

        // Check that the dimension is in the CloudWatchMetrics section
        JsonNode dimensions = rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Dimensions").get(0);
        boolean hasDimension = false;
        for (JsonNode dimension : dimensions) {
            if (dimension.asText().equals("CustomDimension")) {
                hasDimension = true;
                break;
            }
        }
        assertThat(hasDimension).isTrue();
    }

    @Test
    void shouldAddMetadata() throws Exception {
        // When
        metricsLogger.addMetadata("CustomMetadata", "MetadataValue");
        metricsLogger.addMetric("test-metric", 100);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // The metadata is added to the _aws section in the EMF output
        assertThat(rootNode.get("_aws").has("CustomMetadata")).isTrue();
        assertThat(rootNode.get("_aws").get("CustomMetadata").asText()).isEqualTo("MetadataValue");
    }

    @Test
    void shouldSetDefaultDimensions() throws Exception {
        // When
        metricsLogger.setDefaultDimensions(Map.of("Service", "TestService", "Environment", "Test"));
        metricsLogger.addMetric("test-metric", 100);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("TestService");
        assertThat(rootNode.has("Environment")).isTrue();
        assertThat(rootNode.get("Environment").asText()).isEqualTo("Test");
    }

    @Test
    void shouldGetDefaultDimensions() {
        // When
        metricsLogger.setDefaultDimensions(Map.of("Service", "TestService", "Environment", "Test"));
        DimensionSet dimensions = metricsLogger.getDefaultDimensions();

        // Then
        assertThat(dimensions.getDimensions()).containsEntry("Service", "TestService");
        assertThat(dimensions.getDimensions()).containsEntry("Environment", "Test");
    }

    @Test
    void shouldSetNamespace() throws Exception {
        // When
        metricsLogger.setNamespace("CustomNamespace");
        metricsLogger.addMetric("test-metric", 100);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("CustomNamespace");
    }

    @Test
    void shouldRaiseExceptionOnEmptyMetrics() {
        // When
        metricsLogger.setRaiseOnEmptyMetrics(true);

        // Then
        assertThatThrownBy(() -> metricsLogger.flush())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No metrics were emitted");
    }

    @Test
    void shouldClearDefaultDimensions() throws Exception {
        // Given
        metricsLogger.setDefaultDimensions(Map.of("Service", "TestService", "Environment", "Test"));

        // When
        metricsLogger.clearDefaultDimensions();
        metricsLogger.addMetric("test-metric", 100);
        metricsLogger.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Service")).isFalse();
        assertThat(rootNode.has("Environment")).isFalse();
    }

    @Test
    void shouldCaptureColdStartMetric() throws Exception {
        // Given
        Context testContext = new TestContext();

        // When
        metricsLogger.captureColdStartMetric(testContext);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("ColdStart")).isTrue();
        assertThat(rootNode.get("ColdStart").asDouble()).isEqualTo(1.0);
        assertThat(rootNode.has("function_request_id")).isTrue();
        assertThat(rootNode.get("function_request_id").asText()).isEqualTo(testContext.getAwsRequestId());
    }

    @Test
    void shouldCaptureColdStartMetricWithDimensions() throws Exception {
        // Given
        DimensionSet dimensions = DimensionSet.of("CustomDim", "CustomValue");

        // When
        metricsLogger.captureColdStartMetric(dimensions);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("ColdStart")).isTrue();
        assertThat(rootNode.get("ColdStart").asDouble()).isEqualTo(1.0);
        assertThat(rootNode.has("CustomDim")).isTrue();
        assertThat(rootNode.get("CustomDim").asText()).isEqualTo("CustomValue");
    }

    @Test
    void shouldCaptureColdStartMetricWithoutDimensions() throws Exception {
        // When
        metricsLogger.captureColdStartMetric();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("ColdStart")).isTrue();
        assertThat(rootNode.get("ColdStart").asDouble()).isEqualTo(1.0);
    }

    @Test
    void shouldReuseNamespaceForColdStartMetric() throws Exception {
        // Given
        String customNamespace = "CustomNamespace";
        metricsLogger.setNamespace(customNamespace);

        Context testContext = new TestContext();

        DimensionSet dimensions = DimensionSet.of("CustomDim", "CustomValue");

        // When
        metricsLogger.captureColdStartMetric(testContext, dimensions);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("ColdStart")).isTrue();
        assertThat(rootNode.get("ColdStart").asDouble()).isEqualTo(1.0);
        assertThat(rootNode.has("CustomDim")).isTrue();
        assertThat(rootNode.get("CustomDim").asText()).isEqualTo("CustomValue");
        assertThat(rootNode.has("function_request_id")).isTrue();
        assertThat(rootNode.get("function_request_id").asText()).isEqualTo(testContext.getAwsRequestId());
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo(customNamespace);
    }

    @Test
    void shouldPushSingleMetric() throws Exception {
        // Given
        DimensionSet dimensions = DimensionSet.of("CustomDim", "CustomValue");

        // When
        metricsLogger.pushSingleMetric("single-metric", 200, MetricUnit.COUNT, "SingleNamespace", dimensions);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("single-metric")).isTrue();
        assertThat(rootNode.get("single-metric").asDouble()).isEqualTo(200.0);
        assertThat(rootNode.has("CustomDim")).isTrue();
        assertThat(rootNode.get("CustomDim").asText()).isEqualTo("CustomValue");
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("SingleNamespace");
    }

    @Test
    void shouldPushSingleMetricWithoutDimensions() throws Exception {
        // When
        metricsLogger.pushSingleMetric("single-metric", 200, MetricUnit.COUNT, "SingleNamespace");

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("single-metric")).isTrue();
        assertThat(rootNode.get("single-metric").asDouble()).isEqualTo(200.0);
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("SingleNamespace");
    }

}
