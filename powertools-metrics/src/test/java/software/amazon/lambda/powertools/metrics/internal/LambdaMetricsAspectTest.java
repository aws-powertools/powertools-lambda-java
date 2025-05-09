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

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.metrics.ValidationException;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsColdStartEnabledHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledDefaultDimensionHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledDefaultNoDimensionHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledStreamHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsExceptionWhenNoMetricsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsNoDimensionsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsNoExceptionWhenNoMetricsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsTooManyDimensionsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsWithExceptionInHandler;

@SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
public class LambdaMetricsAspectTest {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock
    private Context context;
    private RequestHandler<Object, Object> requestHandler;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        openMocks(this);
        setupContext();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1")
    public void metricsWithoutColdStart() {

        MetricsUtils.defaultDimensions(null);
        requestHandler = new PowertoolsMetricsEnabledHandler();
        requestHandler.handleRequest("input", context);

        assertThat(out.toString().split("\n"))
                .hasSize(2)
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s[0]);

                    assertThat(logAsJson)
                            .containsEntry("Metric2", 1.0)
                            .containsEntry("Dimension1", "Value1")
                            .containsKey("_aws")
                            .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793")
                            .containsEntry("function_request_id", "123ABC");

                    Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                    assertThat(aws.get("CloudWatchMetrics"))
                            .asString()
                            .contains("Namespace=ExampleApplication");

                    logAsJson = readAsJson(s[1]);

                    assertThat(logAsJson)
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1")
    public void metricsWithDefaultDimensionSpecified() {
        requestHandler = new PowertoolsMetricsEnabledDefaultDimensionHandler();

        requestHandler.handleRequest("input", context);

        assertThat(out.toString().split("\n"))
                .hasSize(2)
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s[0]);

                    assertThat(logAsJson)
                            .containsEntry("Metric2", 1.0)
                            .containsEntry("CustomDimension", "booking")
                            .containsKey("_aws")
                            .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793")
                            .containsEntry("function_request_id", "123ABC");

                    Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                    assertThat(aws.get("CloudWatchMetrics"))
                            .asString()
                            .contains("Namespace=ExampleApplication");

                    logAsJson = readAsJson(s[1]);

                    assertThat(logAsJson)
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("CustomDimension", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1")
    public void metricsWithDefaultNoDimensionSpecified() {
        requestHandler = new PowertoolsMetricsEnabledDefaultNoDimensionHandler();

        requestHandler.handleRequest("input", context);

        assertThat(out.toString().split("\n"))
                .hasSize(2)
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s[0]);

                    assertThat(logAsJson)
                            .containsEntry("Metric2", 1.0)
                            .containsKey("_aws")
                            .containsEntry("xray_trace_id", "1-5759e988-bd862e3fe1be46a994272793")
                            .containsEntry("function_request_id", "123ABC");

                    Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                    assertThat(aws.get("CloudWatchMetrics"))
                            .asString()
                            .contains("Namespace=ExampleApplication");

                    logAsJson = readAsJson(s[1]);

                    assertThat(logAsJson)
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void metricsWithColdStart() {
        MetricsUtils.defaultDimensions(null);
        requestHandler = new PowertoolsMetricsColdStartEnabledHandler();

        requestHandler.handleRequest("input", context);

        assertThat(out.toString().split("\n"))
                .hasSize(2)
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s[0]);

                    assertThat(logAsJson)
                            .doesNotContainKey("Metric1")
                            .containsEntry("ColdStart", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");

                    logAsJson = readAsJson(s[1]);

                    assertThat(logAsJson)
                            .doesNotContainKey("ColdStart")
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void noColdStartMetricsWhenColdStartDone() {
        MetricsUtils.defaultDimensions(null);
        requestHandler = new PowertoolsMetricsColdStartEnabledHandler();

        requestHandler.handleRequest("input", context);
        requestHandler.handleRequest("input", context);

        assertThat(out.toString().split("\n"))
                .hasSize(3)
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s[0]);

                    assertThat(logAsJson)
                            .doesNotContainKey("Metric1")
                            .containsEntry("ColdStart", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");

                    logAsJson = readAsJson(s[1]);

                    assertThat(logAsJson)
                            .doesNotContainKey("ColdStart")
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");

                    logAsJson = readAsJson(s[2]);

                    assertThat(logAsJson)
                            .doesNotContainKey("ColdStart")
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void metricsWithStreamHandler() throws IOException {
        MetricsUtils.defaultDimensions(null);
        RequestStreamHandler streamHandler = new PowertoolsMetricsEnabledStreamHandler();

        streamHandler.handleRequest(new ByteArrayInputStream(new byte[] {}), new ByteArrayOutputStream(), context);

        assertThat(out.toString())
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s);

                    assertThat(logAsJson)
                            .containsEntry("Metric1", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void exceptionWhenNoMetricsEmitted() {
        MetricsUtils.defaultDimensions(null);
        requestHandler = new PowertoolsMetricsExceptionWhenNoMetricsHandler();

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> requestHandler.handleRequest("input", context))
                .withMessage("No metrics captured, at least one metrics must be emitted");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void noExceptionWhenNoMetricsEmitted() {
        MetricsUtils.defaultDimensions(null);
        requestHandler = new PowertoolsMetricsNoExceptionWhenNoMetricsHandler();

        requestHandler.handleRequest("input", context);

        assertThat(out.toString())
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s);

                    assertThat(logAsJson)
                            .containsEntry("Service", "booking")
                            .doesNotContainKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void allowWhenNoDimensionsSet() {
        MetricsUtils.defaultDimensions(null);

        requestHandler = new PowertoolsMetricsNoDimensionsHandler();
        requestHandler.handleRequest("input", context);

        assertThat(out.toString())
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s);
                    assertThat(logAsJson)
                            .containsEntry("CoolMetric", 1.0)
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void exceptionWhenTooManyDimensionsSet() {
        MetricsUtils.defaultDimensions(null);

        requestHandler = new PowertoolsMetricsTooManyDimensionsHandler();

        assertThatExceptionOfType(DimensionSetExceededException.class)
                .isThrownBy(() -> requestHandler.handleRequest("input", context))
                .withMessage(
                        "Maximum number of dimensions allowed are 30. Account for default dimensions if not using setDimensions.");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_EMF_ENVIRONMENT", value = "Lambda")
    public void metricsPublishedEvenHandlerThrowsException() {
        MetricsUtils.defaultDimensions(null);
        requestHandler = new PowertoolsMetricsWithExceptionInHandler();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> requestHandler.handleRequest("input", context))
                .withMessage("Whoops, unexpected exception");

        assertThat(out.toString())
                .satisfies(s ->
                {
                    Map<String, Object> logAsJson = readAsJson(s);
                    assertThat(logAsJson)
                            .containsEntry("CoolMetric", 1.0)
                            .containsEntry("Service", "booking")
                            .containsEntry("function_request_id", "123ABC")
                            .containsKey("_aws");
                });
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
        when(context.getAwsRequestId()).thenReturn("123ABC");
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
