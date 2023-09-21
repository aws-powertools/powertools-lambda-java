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

package software.amazon.lambda.powertools.logging.internal;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getenv;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_ARN;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_NAME;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_REQUEST_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_VERSION;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SERVICE;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogAlbCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogApiGatewayHttpApiCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogApiGatewayRestApiCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogClearState;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogDisabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEnabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEvent;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventBridgeCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogSamplingDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogSamplingEnabled;

class LambdaLoggingAspectTest {

    private static final int EXPECTED_CONTEXT_SIZE = 8;
    private RequestStreamHandler requestStreamHandler;
    private RequestHandler<Object, Object> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    @ClearEnvironmentVariable(key = "POWERTOOLS_LOGGER_SAMPLE_RATE")
    void setUp() throws IllegalAccessException, IOException, NoSuchMethodException, InvocationTargetException {
        openMocks(this);
        MDC.clear();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        requestHandler = new PowertoolsLogEnabled();
        requestStreamHandler = new PowertoolsLogEnabledForStream();
        resetLogLevel(Level.INFO);
    }

    @AfterEach
    void cleanUp() throws IOException {
        //Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldSetLambdaContextWhenEnabled() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(FUNCTION_ARN.getName(), "testArn")
                .containsEntry(FUNCTION_MEMORY_SIZE.getName(), "10")
                .containsEntry(FUNCTION_VERSION.getName(), "1")
                .containsEntry(FUNCTION_NAME.getName(), "testFunction")
                .containsEntry(FUNCTION_REQUEST_ID.getName(), "RequestId")
                .containsKey(FUNCTION_COLD_START.getName())
                .containsKey(SERVICE.getName());
    }

    @Test
    void shouldSetLambdaContextForStreamHandlerWhenEnabled() throws IOException {
        requestStreamHandler = new PowertoolsLogEnabledForStream();

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[] {}), new ByteArrayOutputStream(),
                context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(FUNCTION_ARN.getName(), "testArn")
                .containsEntry(FUNCTION_MEMORY_SIZE.getName(), "10")
                .containsEntry(FUNCTION_VERSION.getName(), "1")
                .containsEntry(FUNCTION_NAME.getName(), "testFunction")
                .containsEntry(FUNCTION_REQUEST_ID.getName(), "RequestId")
                .containsKey(FUNCTION_COLD_START.getName())
                .containsKey(SERVICE.getName());
    }

    @Test
    void shouldSetColdStartFlag() throws IOException {
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[] {}), new ByteArrayOutputStream(),
                context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(FUNCTION_COLD_START.getName(), "true");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[] {}), new ByteArrayOutputStream(),
                context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(FUNCTION_COLD_START.getName(), "false");
    }

    @Test
    void shouldNotSetLambdaContextWhenDisabled() {
        requestHandler = new PowertoolsLogDisabled();

        requestHandler.handleRequest(new Object(), context);

        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void shouldNotSetLambdaContextForStreamHandlerWhenDisabled() throws IOException {
        requestStreamHandler = new PowertoolsLogDisabledForStream();

        requestStreamHandler.handleRequest(null, null, context);

        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void shouldClearStateWhenClearStateIsTrue() {
        PowertoolsLogClearState handler = new PowertoolsLogClearState();

        handler.handleRequest(Collections.singletonMap("mySuperSecret", "P@ssw0Rd"), context);

        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void shouldLogDebugWhenSamplingEqualsOne() {
        PowertoolsLogSamplingEnabled handler = new PowertoolsLogSamplingEnabled();
        Boolean debugEnabled = handler.handleRequest(new Object(), context);
        assertThat(debugEnabled).isTrue();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_LOGGER_SAMPLE_RATE", value = "1")
    void shouldLogDebugWhenSamplingEnvVarEqualsOne() {
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
        handler.handleRequest(new Object(), context);
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("Test debug event");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_LOGGER_SAMPLE_RATE", value = "42")
    void shouldNotLogDebugWhenSamplingEnvVarIsTooBig() {
        requestHandler.handleRequest(new Object(), context);
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).doesNotContain("Test debug event");
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_LOGGER_SAMPLE_RATE", value = "NotANumber")
    void shouldNotLogDebugWhenSamplingEnvVarIsInvalid() {
        requestHandler.handleRequest(new Object(), context);
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).doesNotContain("Test debug event");
        assertThat(contentOf(logFile)).contains("Skipping sampling rate on environment variable configuration because of invalid value");
    }

    @Test
    void shouldNotLogDebugWhenSamplingEqualsZero() {
        PowertoolsLogSamplingDisabled handler = new PowertoolsLogSamplingDisabled();
        Boolean debugEnabled = handler.handleRequest(new Object(), context);
        assertThat(debugEnabled).isFalse();
    }

    @Test
    void shouldHaveNoEffectIfNotUsedOnLambdaHandler() {
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();

        handler.anotherMethod();

        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void shouldLogServiceNameWhenEnvVarSet() throws IllegalAccessException {
        writeStaticField(LambdaHandlerProcessor.class, "SERVICE_NAME", "testService", true);
        requestHandler.handleRequest(new Object(), context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(SERVICE.getName(), "testService");
    }

    @Test
    void shouldLogxRayTraceIdEnvVarSet() {
        String xRayTraceId = "1-5759e988-bd862e3fe1be46a994272793";

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1\"");

            requestHandler.handleRequest(new Object(), context);

            assertThat(MDC.getCopyOfContextMap())
                    .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                    .containsEntry(FUNCTION_TRACE_ID.getName(), xRayTraceId);
        }
    }

    @Test
    void shouldLogEventForHandler() throws IOException {
        requestHandler = new PowertoolsLogEvent();

        requestHandler.handleRequest(Collections.singletonList("ListOfOneElement"), context);

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("[\"ListOfOneElement\"]");
    }

    @Test
    void shouldLogEventForStreamHandler() throws IOException {
        requestStreamHandler = new PowertoolsLogEventForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(Collections.singletonMap("key", "value"))), output, context);

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isNotEmpty();

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("{\"key\":\"value\"}");
    }

    @ParameterizedTest
    @Event(value = "apiGatewayProxyEventV1.json", type = APIGatewayProxyRequestEvent.class)
    void shouldLogCorrelationIdOnAPIGatewayProxyRequestEvent(APIGatewayProxyRequestEvent event) {
        RequestHandler<APIGatewayProxyRequestEvent, Object> handler = new PowertoolsLogApiGatewayRestApiCorrelationId();
        handler.handleRequest(event, context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", event.getRequestContext().getRequestId());
    }

    @ParameterizedTest
    @Event(value = "apiGatewayProxyEventV2.json", type = APIGatewayV2HTTPEvent.class)
    void shouldLogCorrelationIdOnAPIGatewayV2HTTPEvent(APIGatewayV2HTTPEvent event) {
        RequestHandler<APIGatewayV2HTTPEvent, Object> handler = new PowertoolsLogApiGatewayHttpApiCorrelationId();
        handler.handleRequest(event, context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", event.getRequestContext().getRequestId());
    }

    @ParameterizedTest
    @Event(value = "albEvent.json", type = ApplicationLoadBalancerRequestEvent.class)
    void shouldLogCorrelationIdOnALBEvent(ApplicationLoadBalancerRequestEvent event) {
        RequestHandler<ApplicationLoadBalancerRequestEvent, Object> handler = new PowertoolsLogAlbCorrelationId();
        handler.handleRequest(event, context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", event.getHeaders().get("x-amzn-trace-id"));
    }

    @Test
    void shouldLogCorrelationIdOnStreamHandler() throws IOException {
        RequestStreamHandler handler = new PowertoolsLogEventBridgeCorrelationId();
        String eventId = "3";
        String event = "{\"id\":" + eventId + "}"; // CorrelationIdPathConstants.EVENT_BRIDGE
        ByteArrayInputStream inputStream = new ByteArrayInputStream(event.getBytes());
        handler.handleRequest(inputStream, new ByteArrayOutputStream(), context);


        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", eventId);
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
        when(context.getAwsRequestId()).thenReturn("RequestId");
    }

    private void resetLogLevel(Level level)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method resetLogLevels = LambdaLoggingAspect.class.getDeclaredMethod("resetLogLevels", Level.class);
        resetLogLevels.setAccessible(true);
        resetLogLevels.invoke(null, level);
        writeStaticField(LambdaLoggingAspect.class, "LEVEL_AT_INITIALISATION", level, true);
    }

}