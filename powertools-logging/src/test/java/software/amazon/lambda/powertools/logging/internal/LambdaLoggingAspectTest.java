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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getProperty;
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
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogAlbCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogApiGatewayHttpApiCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogApiGatewayRestApiCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogAppSyncCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogClearState;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogDisabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEnabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogError;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEvent;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventBridgeCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogResponse;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogResponseForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogSamplingDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogSamplingEnabled;

class LambdaLoggingAspectTest {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaLoggingAspectTest.class);
    private static final int EXPECTED_CONTEXT_SIZE = 8;
    private RequestStreamHandler requestStreamHandler;
    private RequestHandler<Object, Object> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, IOException {
        openMocks(this);
        MDC.clear();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        requestHandler = new PowertoolsLogEnabled();
        requestStreamHandler = new PowertoolsLogEnabledForStream();
        resetLogLevel(Level.INFO);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_EVENT", false, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_SAMPLING_RATE", null, true);
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        //Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldLogDebugWhenPowertoolsLevelEnvVarIsDebug() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "DEBUG", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isTrue();
    }

    @Test
    void shouldLogInfoWhenPowertoolsLevelEnvVarIsInfo() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "INFO", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isTrue();
    }

    @Test
    void shouldLogInfoWhenPowertoolsLevelEnvVarIsInvalid() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "INVALID", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isTrue();
    }

    @Test
    void shouldLogWarnWhenPowertoolsLevelEnvVarIsWarn() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "WARN", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();
    }

    @Test
    void shouldLogErrorWhenPowertoolsLevelEnvVarIsError() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "ERROR", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isFalse();
        assertThat(LOG.isErrorEnabled()).isTrue();
    }

    @Test
    void shouldLogErrorWhenPowertoolsLevelEnvVarIsFatal() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "FATAL", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isFalse();
        assertThat(LOG.isErrorEnabled()).isTrue();
    }

    @Test
    void shouldLogWarnWhenPowertoolsLevelEnvVarIsWarnAndLambdaLevelVarIsInfo() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "WARN", true);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", "INFO", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).doesNotContain(" does not match AWS Lambda Advanced Logging Controls minimum log level");
    }

    @Test
    void shouldLogInfoWhenPowertoolsLevelEnvVarIsInfoAndLambdaLevelVarIsWarn() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", "INFO", true);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", "WARN", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isTrue();
        File logFile = new File("target/logfile.json");
        // should log a warning as powertools level is lower than lambda level
        assertThat(contentOf(logFile)).contains("Current log level (INFO) does not match AWS Lambda Advanced Logging Controls minimum log level (WARN). This can lead to data loss, consider adjusting them.");
    }

    @Test
    void shouldLogWarnWhenPowertoolsLevelEnvVarINotSetAndLambdaLevelVarIsWarn() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", "WARN", true);

        // WHEN
        LambdaLoggingAspect.setLogLevel();

        // THEN
        assertThat(LOG.isDebugEnabled()).isFalse();
        assertThat(LOG.isInfoEnabled()).isFalse();
        assertThat(LOG.isWarnEnabled()).isTrue();
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

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(),
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
    void shouldSetColdStartFlagOnFirstCallNotOnSecondCall() throws IOException {
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(),
                context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(FUNCTION_COLD_START.getName(), "true");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(),
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
    void shouldLogDebugWhenSamplingEnvVarEqualsOne() throws IllegalAccessException {
        // GIVEN
        LoggingConstants.POWERTOOLS_SAMPLING_RATE = "1";
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();

        // WHEN
        handler.handleRequest(new Object(), context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("Test debug event");
    }

    @Test
    void shouldNotLogDebugWhenSamplingEnvVarIsTooBig() throws IllegalAccessException {
        // GIVEN
        LoggingConstants.POWERTOOLS_SAMPLING_RATE = "42";

        // WHEN
        requestHandler.handleRequest(new Object(), context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).doesNotContain("Test debug event");
    }

    @Test
    void shouldNotLogDebugWhenSamplingEnvVarIsInvalid() {
        // GIVEN
        LoggingConstants.POWERTOOLS_SAMPLING_RATE = "NotANumber";

        // WHEN
            requestHandler.handleRequest(new Object(), context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).doesNotContain("Test debug event");
        assertThat(contentOf(logFile)).contains(
                "Skipping sampling rate on environment variable configuration because of invalid value");
    }

    @Test
    void shouldNotLogDebugWhenSamplingEqualsZero() {
        // GIVEN
        LoggingConstants.POWERTOOLS_SAMPLING_RATE = "0";
        PowertoolsLogSamplingDisabled handler = new PowertoolsLogSamplingDisabled();

        // WHEN
        Boolean debugEnabled = handler.handleRequest(new Object(), context);

        // THEN
        assertThat(debugEnabled).isFalse();
    }

    @Test
    void shouldHaveNoEffectIfNotUsedOnLambdaHandler() {
        // GIVEN
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();

        // WHEN
        handler.anotherMethod();

        // THEN
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void shouldLogServiceNameWhenEnvVarSet() throws IllegalAccessException {
        // GIVEN
        writeStaticField(LambdaHandlerProcessor.class, "SERVICE_NAME", "testService", true);

        // WHEN
        requestHandler.handleRequest(new Object(), context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(SERVICE.getName(), "testService");
    }

    @Test
    void shouldLogxRayTraceIdSystemPropertySet() {
        String xRayTraceId = "1-5759e988-bd862e3fe1be46a994272793";

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn(null);
            mocked.when(() -> getProperty("com.amazonaws.xray.traceHeader"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            requestHandler.handleRequest(new Object(), context);

            assertThat(MDC.getCopyOfContextMap())
                    .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                    .containsEntry("xray_trace_id", xRayTraceId);
        }
    }

    @Test
    void shouldLogxRayTraceIdEnvVarSet() {
        // GIVEN
        String xRayTraceId = "1-5759e988-bd862e3fe1be46a994272793";

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            // WHEN
            requestHandler.handleRequest(new Object(), context);

            // THEN
            assertThat(MDC.getCopyOfContextMap())
                    .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                    .containsEntry(FUNCTION_TRACE_ID.getName(), xRayTraceId);
        }
    }

    @Test
    void shouldLogEventForHandlerWithLogEventAnnotation() {
        // GIVEN
        requestHandler = new PowertoolsLogEvent();

        // WHEN
        requestHandler.handleRequest(singletonList("ListOfOneElement"), context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("[\"ListOfOneElement\"]");
    }

    @Test
    void shouldLogEventForHandlerWhenEnvVariableSetToTrue() throws IllegalAccessException {
        try {
            // GIVEN
            LoggingConstants.POWERTOOLS_LOG_EVENT = true;

            requestHandler = new PowertoolsLogEnabled();

            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setBody("body");
            message.setMessageId("1234abcd");
            message.setAwsRegion("eu-west-1");

            // WHEN
            requestHandler.handleRequest(message, context);

            // THEN
            File logFile = new File("target/logfile.json");
            assertThat(contentOf(logFile)).contains("\"body\":\"body\"").contains("\"messageId\":\"1234abcd\"").contains("\"awsRegion\":\"eu-west-1\"");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_EVENT = false;
        }
    }

    @Test
    void shouldNotLogEventForHandlerWhenEnvVariableSetToFalse() throws IOException {
        // GIVEN
        LoggingConstants.POWERTOOLS_LOG_EVENT = false;

        // WHEN
        requestHandler = new PowertoolsLogEventDisabled();
        requestHandler.handleRequest(singletonList("ListOfOneElement"), context);

        // THEN
        Assertions.assertEquals(0,
                Files.lines(Paths.get("target/logfile.json")).collect(joining()).length());
    }

    @Test
    void shouldLogEventForStreamHandler() throws IOException {
        // GIVEN
        requestStreamHandler = new PowertoolsLogEventForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(Collections.singletonMap("key", "value"))), output, context);

        // THEN
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isNotEmpty();

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("{\"key\":\"value\"}");
    }

    @Test
    void shouldLogResponseForHandlerWithLogResponseAnnotation() {
        // GIVEN
        requestHandler = new PowertoolsLogResponse();

        // WHEN
        requestHandler.handleRequest("input", context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("Hola mundo");
    }

    @Test
    void shouldLogResponseForHandlerWhenEnvVariableSetToTrue() throws IllegalAccessException {
        try {
            // GIVEN
            LoggingConstants.POWERTOOLS_LOG_RESPONSE = true;

            requestHandler = new PowertoolsLogEnabled();

            // WHEN
            requestHandler.handleRequest("input", context);

            // THEN
            File logFile = new File("target/logfile.json");
            assertThat(contentOf(logFile)).contains("Bonjour le monde");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_RESPONSE = false;
        }
    }

    @Test
    void shouldLogResponseForStreamHandler() throws IOException {
        // GIVEN
        requestStreamHandler = new PowertoolsLogResponseForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String input = "<user><firstName>Bob</firstName><lastName>The Sponge</lastName></user>";

        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, context);

        // THEN
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isEqualTo(input);

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains(input);
    }

    @Test
    void shouldLogErrorForHandlerWithLogErrorAnnotation() {
        // GIVEN
        requestHandler = new PowertoolsLogError();

        // WHEN
        try {
            requestHandler.handleRequest("input", context);
        } catch (Exception e) {
            // ignore
        }

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("This is an error");
    }

    @Test
    void shouldLogErrorForHandlerWhenEnvVariableSetToTrue() throws IllegalAccessException {
        try {
            // GIVEN
            LoggingConstants.POWERTOOLS_LOG_ERROR = true;

            requestHandler = new PowertoolsLogEnabled(true);

            // WHEN
            try {
                requestHandler.handleRequest("input", context);
            } catch (Exception e) {
                // ignore
            }
            // THEN
            File logFile = new File("target/logfile.json");
            assertThat(contentOf(logFile)).contains("Something went wrong");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_ERROR = false;
        }
    }

    @ParameterizedTest
    @Event(value = "apiGatewayProxyEventV1.json", type = APIGatewayProxyRequestEvent.class)
    void shouldLogCorrelationIdOnAPIGatewayProxyRequestEvent(APIGatewayProxyRequestEvent event) {
        // GIVEN
        RequestHandler<APIGatewayProxyRequestEvent, Object> handler = new PowertoolsLogApiGatewayRestApiCorrelationId();

        // WHEN
        handler.handleRequest(event, context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", event.getRequestContext().getRequestId());
    }

    @ParameterizedTest
    @Event(value = "apiGatewayProxyEventV2.json", type = APIGatewayV2HTTPEvent.class)
    void shouldLogCorrelationIdOnAPIGatewayV2HTTPEvent(APIGatewayV2HTTPEvent event) {
        // GIVEN
        RequestHandler<APIGatewayV2HTTPEvent, Object> handler = new PowertoolsLogApiGatewayHttpApiCorrelationId();

        // WHEN
        handler.handleRequest(event, context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", event.getRequestContext().getRequestId());
    }

    @ParameterizedTest
    @Event(value = "albEvent.json", type = ApplicationLoadBalancerRequestEvent.class)
    void shouldLogCorrelationIdOnALBEvent(ApplicationLoadBalancerRequestEvent event) {
        // GIVEN
        RequestHandler<ApplicationLoadBalancerRequestEvent, Object> handler = new PowertoolsLogAlbCorrelationId();

        // WHEN
        handler.handleRequest(event, context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", event.getHeaders().get("x-amzn-trace-id"));
    }

    @Test
    void shouldLogCorrelationIdOnStreamHandler() throws IOException {
        // GIVEN
        RequestStreamHandler handler = new PowertoolsLogEventBridgeCorrelationId();
        String eventId = "3";
        String event = "{\"id\":" + eventId + "}"; // CorrelationIdPath.EVENT_BRIDGE
        ByteArrayInputStream inputStream = new ByteArrayInputStream(event.getBytes());

        // WHEN
        handler.handleRequest(inputStream, new ByteArrayOutputStream(), context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", eventId);
    }

    @Test
    void shouldLogCorrelationIdOnAppSyncEvent() throws IOException {
        // GIVEN
        RequestStreamHandler handler = new PowertoolsLogAppSyncCorrelationId();
        String eventId = "456";
        String event = "{\"request\":{\"headers\":{\"x-amzn-trace-id\":" + eventId + "}}}"; // CorrelationIdPath.APPSYNC_RESOLVER
        ByteArrayInputStream inputStream = new ByteArrayInputStream(event.getBytes());

        // WHEN
        handler.handleRequest(inputStream, new ByteArrayOutputStream(), context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("correlation_id", eventId);
    }

    @Test
    void testMultipleLoggingManagers_shouldWarnAndSelectFirstOne() throws UnsupportedEncodingException {
        // GIVEN
        List<LoggingManager> list = new ArrayList<>();
        list.add(new TestLoggingManager());
        list.add(new DefautlLoggingManager());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outputStream);

        // WHEN
        LambdaLoggingAspect.getLoggingManager(list, stream);

        // THEN
        String output = outputStream.toString("UTF-8");
        assertThat(output)
                .contains("WARN. Multiple LoggingManagers were found on the classpath")
                .contains("WARN. Make sure to have only one of powertools-logging-log4j OR powertools-logging-logback to your dependencies")
                .contains("WARN. Using the first LoggingManager found on the classpath: [" + list.get(0) + "]");
    }

    @Test
    void testNoLoggingManagers_shouldWarnAndCreateDefault() throws UnsupportedEncodingException {
        // GIVEN
        List<LoggingManager> list = new ArrayList<>();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(outputStream);

        // WHEN
        LoggingManager loggingManager = LambdaLoggingAspect.getLoggingManager(list, stream);

        // THEN
        String output = outputStream.toString("UTF-8");
        assertThat(output)
                .contains("ERROR. No LoggingManager was found on the classpath")
                .contains("ERROR. Applying default LoggingManager: POWERTOOLS_LOG_LEVEL variable is ignored")
                .contains("ERROR. Make sure to add either powertools-logging-log4j or powertools-logging-logback to your dependencies");

        assertThat(loggingManager).isExactlyInstanceOf(DefautlLoggingManager.class);
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
        Method setLogLevels = LambdaLoggingAspect.class.getDeclaredMethod("setLogLevels", Level.class);
        setLogLevels.setAccessible(true);
        setLogLevels.invoke(null, level);
        writeStaticField(LambdaLoggingAspect.class, "LEVEL_AT_INITIALISATION", level, true);
    }
}