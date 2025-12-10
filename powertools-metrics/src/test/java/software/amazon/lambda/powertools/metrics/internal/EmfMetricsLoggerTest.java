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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

class EmfMetricsLoggerTest {

    private Metrics metrics;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() throws Exception {
        // Reset LambdaHandlerProcessor's serviceName
        Method resetServiceName = LambdaHandlerProcessor.class.getDeclaredMethod("resetServiceName");
        resetServiceName.setAccessible(true);
        resetServiceName.invoke(null);

        // Reset isColdStart
        java.lang.reflect.Field coldStartField = LambdaHandlerProcessor.class.getDeclaredField("isColdStart");
        coldStartField.setAccessible(true);
        coldStartField.set(null, null);

        metrics = new EmfMetricsLogger(new EnvironmentProvider(), new MetricsContext());
        metrics.setNamespace("TestNamespace");
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    @ParameterizedTest
    @MethodSource("unitConversionTestCases")
    void shouldConvertMetricUnits(MetricUnit inputUnit, Unit expectedUnit) throws Exception {
        // Given
        // We access using reflection here for simplicity (even though this is not best practice)
        Method convertUnitMethod = EmfMetricsLogger.class.getDeclaredMethod("convertUnit", MetricUnit.class);
        convertUnitMethod.setAccessible(true);

        // When
        Unit actualUnit = (Unit) convertUnitMethod.invoke(metrics, inputUnit);

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
        metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
        metrics.flush();

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
        metrics.addMetric("test-metric", 100, MetricUnit.COUNT, MetricResolution.HIGH);
        metrics.flush();

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
        metrics.clearDefaultDimensions(); // Clear default Service dimension first for easier assertions
        metrics.addDimension("CustomDimension", "CustomValue");
        metrics.addMetric("test-metric", 100);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("CustomDimension")).isTrue();
        assertThat(rootNode.get("CustomDimension").asText()).isEqualTo("CustomValue");

        // Check that the dimension is in the CloudWatchMetrics section
        JsonNode dimensions = rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Dimensions").get(0);
        boolean hasDimension = false;
        for (JsonNode dimension : dimensions) {
            if ("CustomDimension".equals(dimension.asText())) {
                hasDimension = true;
                break;
            }
        }
        assertThat(hasDimension).isTrue();
    }

    @Test
    void shouldSetCustomTimestamp() throws Exception {
        // Given
        Instant customTimestamp = Instant.now();

        // When
        metrics.setTimestamp(customTimestamp);
        metrics.addMetric("test-metric", 100);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("_aws")).isTrue();
        assertThat(rootNode.get("_aws").has("Timestamp")).isTrue();
        assertThat(rootNode.get("_aws").get("Timestamp").asLong()).isEqualTo(customTimestamp.toEpochMilli());
    }

    @Test
    void shouldAddDimensionSet() throws Exception {
        // Given
        DimensionSet dimensionSet = DimensionSet.of("Dim1", "Value1", "Dim2", "Value2");

        // When
        metrics.clearDefaultDimensions(); // Clear default Service dimension first for easier assertions
        metrics.addDimension(dimensionSet);
        metrics.addMetric("test-metric", 100);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Dim1")).isTrue();
        assertThat(rootNode.get("Dim1").asText()).isEqualTo("Value1");
        assertThat(rootNode.has("Dim2")).isTrue();
        assertThat(rootNode.get("Dim2").asText()).isEqualTo("Value2");

        // Check that the dimensions are in the CloudWatchMetrics section
        JsonNode dimensions = rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Dimensions").get(0);
        boolean hasDim1 = false;
        boolean hasDim2 = false;
        for (JsonNode dimension : dimensions) {
            String dimName = dimension.asText();
            if ("Dim1".equals(dimName)) {
                hasDim1 = true;
            } else if ("Dim2".equals(dimName)) {
                hasDim2 = true;
            }
        }
        assertThat(hasDim1).isTrue();
        assertThat(hasDim2).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenDimensionSetIsNull() {
        // When/Then
        assertThatThrownBy(() -> metrics.addDimension(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DimensionSet cannot be null");
    }

    @Test
    void shouldAddMetadata() throws Exception {
        // When
        metrics.addMetadata("CustomMetadata", "MetadataValue");
        metrics.addMetric("test-metric", 100);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // The metadata is added to the _aws section in the EMF output
        assertThat(rootNode.has("CustomMetadata")).isTrue();
        assertThat(rootNode.get("CustomMetadata").asText()).isEqualTo("MetadataValue");
    }

    @Test
    void shouldClearMetadataAfterFlush() throws Exception {
        // Given - Add metadata and flush first time
        metrics.addMetadata("RequestId", "req-123");
        metrics.addMetadata("UserAgent", "test-agent");
        metrics.addMetric("FirstMetric", 1.0);
        metrics.flush();

        // Capture first flush output and reset for second flush
        String firstFlushOutput = outputStreamCaptor.toString().trim();
        outputStreamCaptor.reset();

        // When - Add another metric and flush again using the SAME metrics instance
        metrics.addMetric("SecondMetric", 2.0);
        metrics.flush();

        // Then - Verify first flush had metadata
        JsonNode firstRootNode = objectMapper.readTree(firstFlushOutput);
        assertThat(firstRootNode.has("RequestId")).isTrue();
        assertThat(firstRootNode.get("RequestId").asText()).isEqualTo("req-123");
        assertThat(firstRootNode.has("UserAgent")).isTrue();
        assertThat(firstRootNode.get("UserAgent").asText()).isEqualTo("test-agent");
        assertThat(firstRootNode.has("FirstMetric")).isTrue();

        // Verify second flush does NOT have metadata from first flush
        // The EMF library automatically clears metadata after flush
        String secondFlushOutput = outputStreamCaptor.toString().trim();
        JsonNode secondRootNode = objectMapper.readTree(secondFlushOutput);

        // Metadata should be cleared after first flush by the EMF library
        assertThat(secondRootNode.has("RequestId")).isFalse();
        assertThat(secondRootNode.has("UserAgent")).isFalse();
        assertThat(secondRootNode.has("SecondMetric")).isTrue();
    }

    @Test
    void shouldInheritMetadataInFlushMetricsMethod() throws Exception {
        // Given - Add metadata to the main metrics instance
        metrics.addMetadata("PersistentMetadata", "should-inherit");
        metrics.addMetadata("GlobalContext", "main-instance");

        // When - Use flushMetrics to create a separate metrics context
        metrics.flushMetrics(separateMetrics -> {
            separateMetrics.addMetric("SeparateMetric", 1.0);
            // Don't add any metadata to the separate instance
        });

        // Then - The separate metrics context SHOULD inherit metadata from main instance
        String flushMetricsOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(flushMetricsOutput);

        // The separate metrics should have inherited metadata (this is expected behavior)
        assertThat(rootNode.has("PersistentMetadata")).isTrue();
        assertThat(rootNode.get("PersistentMetadata").asText()).isEqualTo("should-inherit");
        assertThat(rootNode.has("GlobalContext")).isTrue();
        assertThat(rootNode.get("GlobalContext").asText()).isEqualTo("main-instance");
        assertThat(rootNode.has("SeparateMetric")).isTrue();
    }

    @Test
    void shouldSetDefaultDimensions() throws Exception {
        // Given
        DimensionSet dimensionSet = DimensionSet.of("Service", "TestService", "Environment", "Test");

        // When
        metrics.setDefaultDimensions(dimensionSet);
        metrics.addMetric("test-metric", 100);
        metrics.flush();

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
        // Given
        DimensionSet dimensionSet = DimensionSet.of("Service", "TestService", "Environment", "Test");

        // When
        metrics.setDefaultDimensions(dimensionSet);
        DimensionSet dimensions = metrics.getDefaultDimensions();

        // Then
        assertThat(dimensions.getDimensions()).containsEntry("Service", "TestService");
        assertThat(dimensions.getDimensions()).containsEntry("Environment", "Test");
    }

    @Test
    void shouldThrowExceptionWhenDefaultDimensionSetIsNull() {
        // When/Then
        assertThatThrownBy(() -> metrics.setDefaultDimensions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DimensionSet cannot be null");
    }

    @Test
    void shouldSetNamespace() throws Exception {
        // When
        metrics.setNamespace("CustomNamespace");
        metrics.addMetric("test-metric", 100);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("CustomNamespace");
    }

    @Test
    void shouldRaiseExceptionOnEmptyMetrics() {
        // When
        metrics.setRaiseOnEmptyMetrics(true);

        // Then
        assertThatThrownBy(() -> metrics.flush())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No metrics were emitted");
    }

    @Test
    void shouldLogWarningOnEmptyMetrics() throws Exception {
        // Given
        File logFile = new File("target/metrics-test.log");

        // When
        // Flushing without adding metrics
        metrics.flush();

        // Then
        // Read the log file and check for the warning
        String logContent = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
        assertThat(logContent).contains("No metrics were emitted");
        // No EMF output should be generated
        assertThat(outputStreamCaptor.toString().trim()).isEmpty();
    }

    @Test
    void shouldClearDefaultDimensions() throws Exception {
        // Given
        metrics.setDefaultDimensions(DimensionSet.of("Service", "TestService", "Environment", "Test"));

        // When
        metrics.clearDefaultDimensions();
        metrics.addMetric("test-metric", 100);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("Service")).isFalse();
        assertThat(rootNode.has("Environment")).isFalse();
    }

    @Test
    void shouldCaptureColdStartMetric() throws Exception {
        // Given
        Context testContext = new TestLambdaContext();

        // When
        metrics.captureColdStartMetric(testContext);

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
        metrics.captureColdStartMetric(dimensions);

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
        metrics.captureColdStartMetric();

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
        metrics.setNamespace(customNamespace);

        Context testContext = new TestLambdaContext();

        DimensionSet dimensions = DimensionSet.of("CustomDim", "CustomValue");

        // When
        metrics.captureColdStartMetric(testContext, dimensions);

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
    void shouldFlushMetrics() throws Exception {
        // Given
        metrics.setNamespace("MainNamespace");
        metrics.setDefaultDimensions(DimensionSet.of("CustomDim", "CustomValue"));
        metrics.addDimension(DimensionSet.of("CustomDim2", "CustomValue2"));
        metrics.addMetadata("CustomMetadata", "MetadataValue");

        // When
        metrics.flushMetrics(m -> {
            m.addMetric("metric-one", 200, MetricUnit.COUNT);
            m.addMetric("metric-two", 100, MetricUnit.COUNT);
        });

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("metric-one")).isTrue();
        assertThat(rootNode.get("metric-one").asDouble()).isEqualTo(200.0);
        assertThat(rootNode.has("metric-two")).isTrue();
        assertThat(rootNode.get("metric-two").asDouble()).isEqualTo(100);
        assertThat(rootNode.has("CustomDim")).isTrue();
        assertThat(rootNode.get("CustomDim").asText()).isEqualTo("CustomValue");
        assertThat(rootNode.get("CustomDim2")).isNull();
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("MainNamespace");
        assertThat(rootNode.has("CustomMetadata")).isTrue();
        assertThat(rootNode.get("CustomMetadata").asText()).isEqualTo("MetadataValue");
    }

    @Test
    void shouldFlushSingleMetric() throws Exception {
        // Given
        DimensionSet dimensions = DimensionSet.of("CustomDim", "CustomValue");

        // When
        metrics.flushSingleMetric("single-metric", 200, MetricUnit.COUNT, "SingleNamespace", dimensions);

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
    void shouldFlushSingleMetricWithoutDimensions() throws Exception {
        // When
        metrics.flushSingleMetric("single-metric", 200, MetricUnit.COUNT, "SingleNamespace");

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("single-metric")).isTrue();
        assertThat(rootNode.get("single-metric").asDouble()).isEqualTo(200.0);
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("SingleNamespace");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_DISABLED", value = "true")
    void shouldNotFlushMetricsWhenDisabled() {
        // When
        metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
        metrics.flush();

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        assertThat(emfOutput).isEmpty();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_DISABLED", value = "true")
    void shouldNotCaptureColdStartMetricWhenDisabled() {
        // Given
        Context testContext = new TestLambdaContext();

        // When
        metrics.captureColdStartMetric(testContext);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        assertThat(emfOutput).isEmpty();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_DISABLED", value = "true")
    void shouldNotFlushSingleMetricWhenDisabled() {
        // Given
        DimensionSet dimensions = DimensionSet.of("CustomDim", "CustomValue");

        // When
        metrics.flushSingleMetric("single-metric", 200, MetricUnit.COUNT, "SingleNamespace", dimensions);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        assertThat(emfOutput).isEmpty();
    }

    @Test
    void shouldClearCustomDimensionsAfterFlush() throws Exception {
        // Given - Set up default dimensions that should persist
        DimensionSet defaultDimensions = DimensionSet.of("Service", "TestService", "Environment", "Test");
        metrics.setDefaultDimensions(defaultDimensions);

        // First invocation - add custom dimensions and flush
        DimensionSet customDimensions = DimensionSet.of("EXAMPLE_KEY", "EXAMPLE_VALUE");
        metrics.addDimension(customDimensions);
        metrics.addMetric("SERL", 1.0);
        metrics.flush();

        // Capture first flush output
        String firstFlushOutput = outputStreamCaptor.toString().trim();
        outputStreamCaptor.reset(); // Clear for second flush

        // Second invocation - should NOT have custom dimensions from first invocation
        metrics.addMetric("Expected", 1.0);
        metrics.flush();

        // Then - Verify first flush had both default and custom dimensions
        JsonNode firstRootNode = objectMapper.readTree(firstFlushOutput);
        assertThat(firstRootNode.has("Service")).isTrue();
        assertThat(firstRootNode.get("Service").asText()).isEqualTo("TestService");
        assertThat(firstRootNode.has("Environment")).isTrue();
        assertThat(firstRootNode.get("Environment").asText()).isEqualTo("Test");
        assertThat(firstRootNode.has("EXAMPLE_KEY")).isTrue();
        assertThat(firstRootNode.get("EXAMPLE_KEY").asText()).isEqualTo("EXAMPLE_VALUE");
        assertThat(firstRootNode.has("SERL")).isTrue();

        // Verify second flush has ONLY default dimensions (custom dimensions should be cleared)
        String secondFlushOutput = outputStreamCaptor.toString().trim();
        JsonNode secondRootNode = objectMapper.readTree(secondFlushOutput);

        // Default dimensions should still be present
        assertThat(secondRootNode.has("Service")).isTrue();
        assertThat(secondRootNode.get("Service").asText()).isEqualTo("TestService");
        assertThat(secondRootNode.has("Environment")).isTrue();
        assertThat(secondRootNode.get("Environment").asText()).isEqualTo("Test");

        // Custom dimensions should be cleared (this is the failing assertion that demonstrates the bug)
        assertThat(secondRootNode.has("EXAMPLE_KEY")).isFalse();
        assertThat(secondRootNode.has("Expected")).isTrue();

        // Verify dimensions in CloudWatchMetrics section
        JsonNode secondDimensions = secondRootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Dimensions").get(0);
        boolean hasExampleKey = false;
        boolean hasService = false;
        boolean hasEnvironment = false;

        for (JsonNode dimension : secondDimensions) {
            String dimName = dimension.asText();
            if ("EXAMPLE_KEY".equals(dimName)) {
                hasExampleKey = true;
            } else if ("Service".equals(dimName)) {
                hasService = true;
            } else if ("Environment".equals(dimName)) {
                hasEnvironment = true;
            }
        }

        // Default dimensions should be in CloudWatchMetrics
        assertThat(hasService).isTrue();
        assertThat(hasEnvironment).isTrue();
        // Custom dimension should NOT be in CloudWatchMetrics (this should fail initially)
        assertThat(hasExampleKey).isFalse();
    }

    @Test
    void shouldHandleEmptyCustomDimensionsGracefully() throws Exception {
        // Given - Only default dimensions, no custom dimensions
        metrics.setDefaultDimensions(DimensionSet.of("Service", "TestService"));

        // When - Flush without adding custom dimensions
        metrics.addMetric("TestMetric", 1.0);
        metrics.flush();
        outputStreamCaptor.reset();

        // Second flush
        metrics.addMetric("TestMetric2", 2.0);
        metrics.flush();

        // Then - Should work normally with only default dimensions
        String output = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(output);

        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("TestService");
        assertThat(rootNode.has("TestMetric2")).isTrue();
    }

    @Test
    void shouldClearCustomDimensionsWhenNoDefaultDimensionsSet() throws Exception {
        // Given - No default dimensions set
        metrics.clearDefaultDimensions();

        // When - Add custom dimensions and flush
        metrics.addDimension("CustomDim", "CustomValue");
        metrics.addMetric("Metric1", 1.0);
        metrics.flush();
        outputStreamCaptor.reset();

        // Second flush without custom dimensions
        metrics.addMetric("Metric2", 2.0);
        metrics.flush();

        // Then - Custom dimensions should be cleared
        String output = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(output);

        assertThat(rootNode.has("CustomDim")).isFalse();
        assertThat(rootNode.has("Metric2")).isTrue();

        // Verify no custom dimensions in CloudWatchMetrics section
        JsonNode dimensionsArray = rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Dimensions");
        boolean hasCustomDim = false;
        if (dimensionsArray != null && dimensionsArray.size() > 0) {
            JsonNode dimensions = dimensionsArray.get(0);
            if (dimensions != null) {
                for (JsonNode dimension : dimensions) {
                    if ("CustomDim".equals(dimension.asText())) {
                        hasCustomDim = true;
                        break;
                    }
                }
            }
        }
        assertThat(hasCustomDim).isFalse();
    }
}
