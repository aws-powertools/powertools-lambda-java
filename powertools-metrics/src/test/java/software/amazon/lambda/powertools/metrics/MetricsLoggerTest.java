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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mockStatic;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getProperty;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getenv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;

class MetricsLoggerTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
        }
    }

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void singleMetricsCaptureUtilityWithDefaultDimension() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.common.internal.SystemWrapper> internalWrapper = mockStatic(
                     software.amazon.lambda.powertools.common.internal.SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            internalWrapper.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            MetricsUtils.defaultDimensions(DimensionSet.of("Service", "Booking"));

            MetricsUtils.withSingleMetric("Metric1", 1, Unit.COUNT, "test",
                    metricsLogger ->
                    {
                    });

            assertThat(out.toString())
                    .satisfies(s ->
                    {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Service", "Booking")
                                .containsKey("_aws")
                                .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793");
                    });
        }
    }

    @Test
    void singleMetricsCaptureUtility() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.common.internal.SystemWrapper> internalWrapper = mockStatic(
                     software.amazon.lambda.powertools.common.internal.SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            internalWrapper.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            MetricsUtils.withSingleMetric("Metric1", 1, Unit.COUNT, "test",
                    metricsLogger -> metricsLogger.setDimensions(DimensionSet.of("Dimension1", "Value1")));

            assertThat(out.toString())
                    .satisfies(s ->
                    {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Dimension1", "Value1")
                                .containsKey("_aws")
                                .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793");

                        Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                        assertThat(aws.get("CloudWatchMetrics"))
                                .asString()
                                .contains("Namespace=test");
                    });
        }
    }

    @Test
    void singleMetricsCaptureUtilityWithNullNamespace() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.common.internal.SystemWrapper> internalWrapper = mockStatic(
                     software.amazon.lambda.powertools.common.internal.SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            // POWERTOOLS_METRICS_NAMESPACE is not defined

            MetricsUtils.withSingleMetric("Metric1", 1, Unit.COUNT,
                    metricsLogger -> metricsLogger.setDimensions(DimensionSet.of("Dimension1", "Value1")));

            assertThat(out.toString())
                    .satisfies(s ->
                    {
                        Map<String, Object> logAsJson = readAsJson(s);

                        Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                        assertThat(aws.get("CloudWatchMetrics"))
                                .asString()
                                .contains("Namespace=aws-embedded-metrics");
                    });
        }
    }

    @Test
    void singleMetricsCaptureUtilityWithDefaultNameSpace() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.common.internal.SystemWrapper> internalWrapper = mockStatic(
                     software.amazon.lambda.powertools.common.internal.SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            mocked.when(() -> SystemWrapper.getenv("POWERTOOLS_METRICS_NAMESPACE")).thenReturn("GlobalName");
            internalWrapper.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            MetricsUtils.withSingleMetric("Metric1", 1, Unit.COUNT,
                    metricsLogger -> metricsLogger.setDimensions(DimensionSet.of("Dimension1", "Value1")));

            assertThat(out.toString())
                    .satisfies(s ->
                    {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Dimension1", "Value1")
                                .containsKey("_aws")
                                .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793");

                        Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                        assertThat(aws.get("CloudWatchMetrics"))
                                .asString()
                                .contains("Namespace=GlobalName");
                    });
        }
    }

    @Test
    void metricsLoggerCaptureUtilityWithDefaultNameSpace() {
        testLogger(MetricsUtils::withMetricsLogger);
    }

    @Test
    void shouldUseTraceIdFromSystemPropertyIfEnvVarNotPresent() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.common.internal.SystemWrapper> internalWrapper = mockStatic(
                     software.amazon.lambda.powertools.common.internal.SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            mocked.when(() -> SystemWrapper.getenv("POWERTOOLS_METRICS_NAMESPACE")).thenReturn("GlobalName");
            internalWrapper.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn(null);
            internalWrapper.when(() -> getProperty("com.amazonaws.xray.traceHeader"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            MetricsUtils.withSingleMetric("Metric1", 1, Unit.COUNT,
                    metricsLogger -> metricsLogger.setDimensions(DimensionSet.of("Dimension1", "Value1")));

            assertThat(out.toString())
                    .satisfies(s ->
                    {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Dimension1", "Value1")
                                .containsKey("_aws")
                                .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793");

                        Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                        assertThat(aws.get("CloudWatchMetrics"))
                                .asString()
                                .contains("Namespace=GlobalName");
                    });
        }
    }

    private void testLogger(Consumer<Consumer<MetricsLogger>> methodToTest) {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.common.internal.SystemWrapper> internalWrapper = mockStatic(
                     software.amazon.lambda.powertools.common.internal.SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            mocked.when(() -> SystemWrapper.getenv("POWERTOOLS_METRICS_NAMESPACE")).thenReturn("GlobalName");
            internalWrapper.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            methodToTest.accept(metricsLogger ->
            {
                metricsLogger.setDimensions(DimensionSet.of("Dimension1", "Value1"));
                metricsLogger.putMetric("Metric1", 1, Unit.COUNT);
                metricsLogger.putMetric("Metric2", 1, Unit.COUNT, StorageResolution.HIGH);
            });

            assertThat(out.toString())
                    .satisfies(s ->
                    {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Dimension1", "Value1")
                                .containsKey("_aws")
                                .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793");

                        Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                        assertThat(aws.get("CloudWatchMetrics"))
                                .asString()
                                .contains("Namespace=GlobalName");

                        ArrayList cloudWatchMetrics = (ArrayList) aws.get("CloudWatchMetrics");
                        LinkedHashMap<String, Object> values =
                                (java.util.LinkedHashMap<String, Object>) cloudWatchMetrics.get(0);
                        ArrayList metricArray = (ArrayList) values.get("Metrics");
                        LinkedHashMap<String, Object> metricValues = (LinkedHashMap<String, Object>) metricArray.get(1);
                        assertThat(metricValues).containsEntry("StorageResolution", 1);
                    });
        }
    }

    private Map<String, Object> readAsJson(String s) {
        try {
            return mapper.readValue(s, Map.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return emptyMap();
    }
}