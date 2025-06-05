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
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.testutils.TestContext;

/**
 * Tests to verify the hierarchy of precedence for configuration:
 * 1. Metrics annotation
 * 2. MetricsLoggerBuilder
 * 3. Environment variables
 */
class ConfigurationPrecedenceTest {

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

        field = MetricsLoggerFactory.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(null, new software.amazon.lambda.powertools.metrics.provider.EmfMetricsProvider());
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = "EnvNamespace")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "EnvService")
    void annotationShouldOverrideBuilderAndEnvironment() throws Exception {
        // Given
        // Configure with builder first
        MetricsLoggerBuilder.builder()
                .withNamespace("BuilderNamespace")
                .withService("BuilderService")
                .build();

        RequestHandler<Map<String, Object>, String> handler = new HandlerWithMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // Annotation values should take precedence
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("AnnotationNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("AnnotationService");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = "EnvNamespace")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "EnvService")
    void builderShouldOverrideEnvironment() throws Exception {
        // Given
        // Configure with builder
        MetricsLoggerBuilder.builder()
                .withNamespace("BuilderNamespace")
                .withService("BuilderService")
                .build();

        RequestHandler<Map<String, Object>, String> handler = new HandlerWithDefaultMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // Builder values should take precedence over environment variables
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("BuilderNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("BuilderService");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = "EnvNamespace")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "EnvService")
    void environmentVariablesShouldBeUsedWhenNoOverrides() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithDefaultMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // Environment variable values should be used
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("EnvNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("EnvService");
    }

    @Test
    void shouldUseDefaultsWhenNoConfiguration() throws Exception {
        // Given
        MetricsLoggerBuilder.builder()
                .withNamespace("TestNamespace")
                .build();

        RequestHandler<Map<String, Object>, String> handler = new HandlerWithDefaultMetricsAnnotation();
        Context context = new TestContext();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, context);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        // Default values should be used
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("TestNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("service_undefined");
    }

    private static class HandlerWithMetricsAnnotation implements RequestHandler<Map<String, Object>, String> {
        @Override
        @Metrics(namespace = "AnnotationNamespace", service = "AnnotationService")
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

    private static class HandlerWithDefaultMetricsAnnotation implements RequestHandler<Map<String, Object>, String> {
        @Override
        @Metrics
        public String handleRequest(Map<String, Object> input, Context context) {
            MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();
            metricsLogger.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }

}
