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
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_ARN;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_NAME;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_REQUEST_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_VERSION;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SERVICE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.slf4j.test.TestLogger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.logging.argument.StructuredArgument;
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
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogErrorNoFlush;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEvent;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventBridgeCorrelationId;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventEnvVar;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogEventForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogResponse;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogResponseForStream;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogSamplingDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowertoolsLogSamplingEnabled;

class LambdaLoggingAspectTest {

    private static final int EXPECTED_CONTEXT_SIZE = 8;
    private RequestStreamHandler requestStreamHandler;
    private RequestHandler<Object, Object> requestHandler;

    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException, IOException {
        MDC.clear();

        // Reset cold start state
        writeStaticField(LambdaHandlerProcessor.class, "isColdStart", null, true);
        writeStaticField(PowertoolsLogging.class, "hasBeenInitialized", false, true);

        context = new TestLambdaContext();
        requestHandler = new PowertoolsLogEnabled();
        requestStreamHandler = new PowertoolsLogEnabledForStream();
        writeStaticField(LoggingConstants.class, "LAMBDA_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_LEVEL", null, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_LOG_EVENT", false, true);
        writeStaticField(LoggingConstants.class, "POWERTOOLS_SAMPLING_RATE", null, true);

        // Reset buffer state for clean test isolation
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            ((TestLoggingManager) loggingManager).resetBufferState();
        }

        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        // Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();

        // Reset log level to INFO to ensure test isolation
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        loggingManager.setLogLevel(Level.INFO);
    }

    @Test
    void shouldSetLambdaContextWhenEnabled() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(FUNCTION_ARN.getName(), "arn:aws:lambda:us-east-1:123456789012:function:test")
                .containsEntry(FUNCTION_MEMORY_SIZE.getName(), "128")
                .containsEntry(FUNCTION_VERSION.getName(), "1")
                .containsEntry(FUNCTION_NAME.getName(), "test-function")
                .containsEntry(FUNCTION_REQUEST_ID.getName(), "test-request-id")
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
                .containsEntry(FUNCTION_ARN.getName(), "arn:aws:lambda:us-east-1:123456789012:function:test")
                .containsEntry(FUNCTION_MEMORY_SIZE.getName(), "128")
                .containsEntry(FUNCTION_VERSION.getName(), "1")
                .containsEntry(FUNCTION_NAME.getName(), "test-function")
                .containsEntry(FUNCTION_REQUEST_ID.getName(), "test-request-id")
                .containsKey(FUNCTION_COLD_START.getName())
                .containsKey(SERVICE.getName());
    }

    @Test
    void shouldSetColdStartFlagOnFirstCallNotOnSecondCall() throws IOException {
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
    void shouldLogDebugWhenSamplingEnvVarEqualsOne() {
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
    void shouldNotLogDebugWhenSamplingEnvVarIsTooBig() {
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
        writeStaticField(LambdaHandlerProcessor.class, "serviceName", "testService", true);

        // WHEN
        requestHandler.handleRequest(new Object(), context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(SERVICE.getName(), "testService");
    }

    @Test
    @ClearEnvironmentVariable(key = "_X_AMZN_TRACE_ID")
    @SetSystemProperty(key = "com.amazonaws.xray.traceHeader", value = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1")
    void shouldLogxRayTraceIdSystemPropertySet() {
        String xRayTraceId = "1-5759e988-bd862e3fe1be46a994272793";

        requestHandler.handleRequest(new Object(), context);

        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry("xray_trace_id", xRayTraceId);
    }

    @Test
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1")
    void shouldLogxRayTraceIdEnvVarSet() {
        // GIVEN
        String xRayTraceId = "1-5759e988-bd862e3fe1be46a994272793";

        // WHEN
        requestHandler.handleRequest(new Object(), context);

        // THEN
        assertThat(MDC.getCopyOfContextMap())
                .hasSize(EXPECTED_CONTEXT_SIZE + 1)
                .containsEntry(FUNCTION_TRACE_ID.getName(), xRayTraceId);
    }

    @Test
    void shouldLogEventForHandlerWithLogEventAnnotation() {
        // GIVEN
        requestHandler = new PowertoolsLogEvent();
        List<String> listOfOneElement = singletonList("ListOfOneElement");

        // WHEN
        requestHandler.handleRequest(listOfOneElement, context);

        // THEN
        TestLogger logger = (TestLogger) PowertoolsLogEvent.getLogger();
        assertThat(logger.getArguments()).hasSize(1);
        StructuredArgument argument = (StructuredArgument) logger.getArguments()[0];
        assertThat(argument.toString()).hasToString("event=" + listOfOneElement.toString());
    }

    @Test
    void shouldLogEventForHandlerWhenEnvVariableSetToTrue() {
        // GIVEN
        LoggingConstants.POWERTOOLS_LOG_EVENT = true;

        requestHandler = new PowertoolsLogEventEnvVar();

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody("body");
        message.setMessageId("1234abcd");
        message.setAwsRegion("eu-west-1");

        // WHEN
        requestHandler.handleRequest(message, context);

        // THEN
        TestLogger logger = (TestLogger) ((PowertoolsLogEventEnvVar) requestHandler).getLogger();
        try {
            assertThat(logger.getArguments()).hasSize(1);
            StructuredArgument argument = (StructuredArgument) logger.getArguments()[0];
            assertThat(argument.toString()).hasToString("event={messageId: 1234abcd,awsRegion: eu-west-1,body: body,}");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_EVENT = false;
            if (logger != null) {
                logger.clearArguments();
            }
        }
    }

    @Test
    void shouldNotLogEventForHandlerWhenEnvVariableSetToFalse() {
        // GIVEN
        LoggingConstants.POWERTOOLS_LOG_EVENT = false;

        // WHEN
        requestHandler = new PowertoolsLogEventEnvVar();
        requestHandler.handleRequest(singletonList("ListOfOneElement"), context);

        // THEN
        TestLogger logger = (TestLogger) ((PowertoolsLogEventEnvVar) requestHandler).getLogger();
        assertThat(logger.getArguments()).isNull();
    }

    @Test
    void shouldLogEventForStreamHandler() throws IOException {
        // GIVEN
        requestStreamHandler = new PowertoolsLogEventForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Map<String, String> map = Collections.singletonMap("key", "value");

        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(map)), output,
                context);

        // THEN
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isNotEmpty();

        TestLogger logger = (TestLogger) PowertoolsLogEventForStream.getLogger();
        try {
            assertThat(logger.getArguments()).hasSize(1);
            StructuredArgument argument = (StructuredArgument) logger.getArguments()[0];
            assertThat(argument.toString()).hasToString("event={\"key\":\"value\"}");
        } finally {
            logger.clearArguments();
        }
    }

    @Test
    void shouldLogResponseForHandlerWithLogResponseAnnotation() {
        // GIVEN
        requestHandler = new PowertoolsLogResponse();

        // WHEN
        requestHandler.handleRequest("input", context);

        // THEN
        TestLogger logger = (TestLogger) PowertoolsLogResponse.getLogger();
        try {
            assertThat(logger.getArguments()).hasSize(1);
            StructuredArgument argument = (StructuredArgument) logger.getArguments()[0];
            assertThat(argument.toString()).hasToString("response=Hola mundo");
        } finally {
            logger.clearArguments();
        }
    }

    @Test
    void shouldLogResponseForHandlerWhenEnvVariableSetToTrue() {
        // GIVEN
        LoggingConstants.POWERTOOLS_LOG_RESPONSE = true;

        requestHandler = new PowertoolsLogEnabled();

        // WHEN
        requestHandler.handleRequest("input", context);

        // THEN
        TestLogger logger = (TestLogger) PowertoolsLogEnabled.getLogger();
        try {
            assertThat(logger.getArguments()).hasSize(1);
            StructuredArgument argument = (StructuredArgument) logger.getArguments()[0];
            assertThat(argument.toString()).hasToString("response=Bonjour le monde");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_RESPONSE = false;
            logger.clearArguments();
        }
    }

    @Test
    void shouldLogResponseForStreamHandler() throws IOException {
        // GIVEN
        requestStreamHandler = new PowertoolsLogResponseForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String input = "<user><firstName>Bob</firstName><lastName>The Sponge</lastName></user>";

        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output,
                context);

        // THEN
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isEqualTo(input);

        TestLogger logger = (TestLogger) PowertoolsLogResponseForStream.getLogger();
        try {
            assertThat(logger.getArguments()).hasSize(1);
            StructuredArgument argument = (StructuredArgument) logger.getArguments()[0];
            assertThat(argument.toString()).hasToString("response=" + input);
        } finally {
            logger.clearArguments();
        }
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
    void shouldLogErrorForHandlerWhenEnvVariableSetToTrue() {
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
    void shouldClearBufferAfterSuccessfulHandlerExecution() {
        // WHEN
        requestHandler.handleRequest(new Object(), context);

        // THEN
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            assertThat(((TestLoggingManager) loggingManager).isBufferCleared()).isTrue();
        }
    }

    @Test
    void shouldClearBufferAfterSuccessfulStreamHandlerExecution() throws IOException {
        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[] {}), new ByteArrayOutputStream(),
                context);

        // THEN
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            assertThat(((TestLoggingManager) loggingManager).isBufferCleared()).isTrue();
        }
    }

    @Test
    void shouldClearBufferAfterHandlerExceptionWithLogError() {
        // GIVEN
        requestHandler = new PowertoolsLogError();

        // WHEN
        try {
            requestHandler.handleRequest("input", context);
        } catch (Exception e) {
            // ignore
        }

        // THEN
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            assertThat(((TestLoggingManager) loggingManager).isBufferCleared()).isTrue();
        }
    }

    @Test
    void shouldClearBufferAfterHandlerExceptionWithEnvVarLogError() {
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
            LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
            if (loggingManager instanceof TestLoggingManager) {
                assertThat(((TestLoggingManager) loggingManager).isBufferCleared()).isTrue();
            }
        } finally {
            LoggingConstants.POWERTOOLS_LOG_ERROR = false;
        }
    }

    @Test
    void shouldFlushBufferOnUncaughtErrorWhenEnabled() {
        // GIVEN
        requestHandler = new PowertoolsLogError();

        // WHEN
        try {
            requestHandler.handleRequest("input", context);
        } catch (Exception e) {
            // ignore
        }

        // THEN
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            assertThat(((TestLoggingManager) loggingManager).isBufferFlushed()).isTrue();
        }
    }

    @Test
    void shouldNotFlushBufferOnUncaughtErrorWhenDisabled() {
        // GIVEN
        PowertoolsLogErrorNoFlush handler = new PowertoolsLogErrorNoFlush();

        // WHEN
        try {
            handler.handleRequest("input", context);
        } catch (Exception e) {
            // ignore
        }

        // THEN
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            assertThat(((TestLoggingManager) loggingManager).isBufferFlushed()).isFalse();
        }
    }

    @Test
    void shouldClearBufferBeforeErrorLoggingWhenFlushBufferOnUncaughtErrorDisabled() {
        // GIVEN
        PowertoolsLogErrorNoFlush handler = new PowertoolsLogErrorNoFlush();

        // WHEN
        try {
            handler.handleRequest("input", context);
        } catch (Exception e) {
            // ignore
        }

        // THEN - Buffer should be cleared and not flushed
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof TestLoggingManager) {
            assertThat(((TestLoggingManager) loggingManager).isBufferCleared()).isTrue();
            assertThat(((TestLoggingManager) loggingManager).isBufferFlushed()).isFalse();
        }
    }

}
