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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.testutils.TestContext;

class LambdaMetricsAspectTest {

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
        java.lang.reflect.Field field = MetricsLoggerFactory.class.getDeclaredField("metricsLogger");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void shouldCaptureMetricsFromAnnotatedHandler() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("test-metric")).isTrue();
        assertThat(rootNode.get("test-metric").asDouble()).isEqualTo(100.0);
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("CustomNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("CustomService");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = "EnvNamespace")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "EnvService")
    void shouldOverrideEnvironmentVariablesWithAnnotation() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("CustomNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("CustomService");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = "EnvNamespace")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "EnvService")
    void shouldUseEnvironmentVariablesWhenNoAnnotationOverrides() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithDefaultMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("EnvNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("EnvService");
    }

    @Test
    void shouldCaptureColdStartMetricWhenConfigured() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithColdStartMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        String[] emfOutputs = emfOutput.split("\\n");

        // There should be two EMF outputs - one for cold start and one for the handler metrics
        assertThat(emfOutputs).hasSize(2);

        JsonNode coldStartNode = objectMapper.readTree(emfOutputs[0]);
        assertThat(coldStartNode.has("ColdStart")).isTrue();
        assertThat(coldStartNode.get("ColdStart").asDouble()).isEqualTo(1.0);

        JsonNode metricsNode = objectMapper.readTree(emfOutputs[1]);
        assertThat(metricsNode.has("test-metric")).isTrue();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_FUNCTION_NAME", value = "EnvFunctionName")
    void shouldNotIncludeServiceDimensionInColdStartMetricWhenServiceUndefined() throws Exception {
        // Given - no service name set, so it will use the default undefined value
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithColdStartMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        String[] emfOutputs = emfOutput.split("\\n");

        // There should be two EMF outputs - one for cold start and one for the handler metrics
        assertThat(emfOutputs).hasSize(2);

        JsonNode coldStartNode = objectMapper.readTree(emfOutputs[0]);
        assertThat(coldStartNode.has("ColdStart")).isTrue();
        assertThat(coldStartNode.get("ColdStart").asDouble()).isEqualTo(1.0);

        // Service dimension should not be present in cold start metrics
        assertThat(coldStartNode.has("Service")).isFalse();

        // FunctionName dimension should be present
        assertThat(coldStartNode.has("FunctionName")).isTrue();
        assertThat(coldStartNode.get("FunctionName").asText()).isEqualTo("EnvFunctionName");
    }

    @Test
    void shouldUseCustomFunctionNameWhenProvidedForColdStartMetric() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithCustomFunctionName();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        String[] emfOutputs = emfOutput.split("\\n");

        // There should be two EMF outputs - one for cold start and one for the handler metrics
        assertThat(emfOutputs).hasSize(2);

        JsonNode coldStartNode = objectMapper.readTree(emfOutputs[0]);
        assertThat(coldStartNode.has("FunctionName")).isTrue();
        assertThat(coldStartNode.get("FunctionName").asText()).isEqualTo("CustomFunction");

        // Check that FunctionName is in the dimensions
        JsonNode dimensions = coldStartNode.get("_aws").get("CloudWatchMetrics").get(0).get("Dimensions").get(0);
        boolean hasFunctionName = false;
        for (JsonNode dimension : dimensions) {
            if (dimension.asText().equals("FunctionName")) {
                hasFunctionName = true;
                break;
            }
        }
        assertThat(hasFunctionName).isTrue();
    }

    @Test
    void shouldUseServiceNameWhenProvidedForColdStartMetric() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithServiceNameAndColdStart();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // Should use the service name from annotation
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("CustomService");
    }

    @Test
    void shouldHaveNoEffectOnNonHandlerMethod() {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithAnnotationOnWrongMethod();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();

        // Should be empty because we do not flush any metrics manually
        assertThat(emfOutput).isEmpty();
    }

    static class HandlerWithMetricsAnnotation implements RequestHandler<Map<String, Object>, String> {
        @Override
        @FlushMetrics(namespace = "CustomNamespace", service = "CustomService")
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

    static class HandlerWithDefaultMetricsAnnotation implements RequestHandler<Map<String, Object>, String> {
        @Override
        @FlushMetrics
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

    static class HandlerWithColdStartMetricsAnnotation implements RequestHandler<Map<String, Object>, String> {
        @Override
        @FlushMetrics(captureColdStart = true, namespace = "TestNamespace")
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

    static class HandlerWithCustomFunctionName implements RequestHandler<Map<String, Object>, String> {
        @Override
        @FlushMetrics(captureColdStart = true, functionName = "CustomFunction", namespace = "TestNamespace")
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

    static class HandlerWithServiceNameAndColdStart implements RequestHandler<Map<String, Object>, String> {
        @Override
        @FlushMetrics(service = "CustomService", captureColdStart = true, namespace = "TestNamespace")
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

    static class HandlerWithAnnotationOnWrongMethod implements RequestHandler<Map<String, Object>, String> {
        @Override
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            someOtherMethod();
            return "OK";
        }

        @FlushMetrics
        public void someOtherMethod() {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
        }
    }
}
