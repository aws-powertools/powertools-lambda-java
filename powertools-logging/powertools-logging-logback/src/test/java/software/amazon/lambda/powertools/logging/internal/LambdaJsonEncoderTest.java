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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.argument.StructuredArgument;
import software.amazon.lambda.powertools.logging.argument.StructuredArguments;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsArguments;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEvent;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEventDisabled;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEventForStream;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogResponse;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogResponseForStream;
import software.amazon.lambda.powertools.logging.logback.LambdaJsonEncoder;

@Order(2)
class LambdaJsonEncoderTest {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(LambdaJsonEncoderTest.class.getName());

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException, IOException {
        openMocks(this);
        MDC.clear();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        // Make sure file is cleaned up before running tests
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // file may not exist on the first launch
        }
    }

    @AfterEach
    void cleanUp() throws IOException{
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldLogInJsonFormat() {
        // GIVEN
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();

        // WHEN
        handler.handleRequest("Input", context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains(
                        "{\"level\":\"DEBUG\",\"message\":\"Test debug event\",\"cold_start\":true,\"function_arn\":\"arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1\",\"function_memory_size\":1024,\"function_name\":\"testFunction\",\"function_request_id\":\"RequestId\",\"function_version\":1,\"service\":\"testLogback\",\"xray_trace_id\":\"1-63441c4a-abcdef012345678912345678\",\"myKey\":\"myValue\",\"timestamp\":");
    }

    @Test
    void shouldLogArgumentsAsJsonWhenUsingRawJson() {
        // GIVEN
        PowertoolsArguments requestHandler = new PowertoolsArguments(PowertoolsArguments.ArgumentFormat.JSON);
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("1212abcd");
        msg.setBody("plop");
        msg.setEventSource("eb");
        msg.setAwsRegion("eu-west-1");
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setStringListValues(Arrays.asList("val1", "val2", "val3"));
        msg.setMessageAttributes(Collections.singletonMap("keyAttribute", attribute));

        // WHEN
        requestHandler.handleRequest(msg, context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains("\"input\":{\"awsRegion\":\"eu-west-1\",\"body\":\"plop\",\"eventSource\":\"eb\",\"messageAttributes\":{\"keyAttribute\":{\"stringListValues\":[\"val1\",\"val2\",\"val3\"]}},\"messageId\":\"1212abcd\"}")
                .contains("\"message\":\"1212abcd\"")
                // Should auto-escape double quotes around id
                .contains("\"message\":\"Message body = plop and id = \\\"1212abcd\\\"\"");
        // Reserved keys should be ignored
        PowertoolsLoggedFields.stringValues().stream().forEach(reservedKey -> {
            assertThat(contentOf(logFile)).doesNotContain("\"" + reservedKey + "\":\"shouldBeIgnored\"");
            assertThat(contentOf(logFile)).contains(
                    "\"message\":\"Attempted to use reserved key '" + reservedKey
                            + "' in structured argument. This key will be ignored.\"");
        });
    }

    @Test
    void shouldLogArgumentsAsJsonWhenUsingKeyValue() {
        // GIVEN
        PowertoolsArguments requestHandler = new PowertoolsArguments(PowertoolsArguments.ArgumentFormat.ENTRY);
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("1212abcd");
        msg.setBody("plop");
        msg.setEventSource("eb");
        msg.setAwsRegion("eu-west-1");
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setStringListValues(Arrays.asList("val1", "val2", "val3"));
        msg.setMessageAttributes(Collections.singletonMap("keyAttribute", attribute));

        // WHEN
        requestHandler.handleRequest(msg, context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains("\"input\":{\"awsRegion\":\"eu-west-1\",\"body\":\"plop\",\"eventSource\":\"eb\",\"messageAttributes\":{\"keyAttribute\":{\"stringListValues\":[\"val1\",\"val2\",\"val3\"]}},\"messageId\":\"1212abcd\"}")
                .contains("\"message\":\"1212abcd\"")
                // Should auto-escape double quotes around id
                .contains("\"message\":\"Message body = plop and id = \\\"1212abcd\\\"\"");
        // Reserved keys should be ignored
        PowertoolsLoggedFields.stringValues().stream().forEach(reservedKey -> {
            assertThat(contentOf(logFile)).doesNotContain("\"" + reservedKey + "\":\"shouldBeIgnored\"");
            assertThat(contentOf(logFile)).contains(
                    "\"message\":\"Attempted to use reserved key '" + reservedKey
                            + "' in structured argument. This key will be ignored.\"");
        });
    }

    private final LoggingEvent loggingEvent = new LoggingEvent("fqcn", logger, Level.INFO, "message", null, null);

    @Test
    void shouldNotLogPowertoolsInfo() {
        // GIVEN
        LambdaJsonEncoder encoder = new LambdaJsonEncoder();

        MDC.put(PowertoolsLoggedFields.FUNCTION_NAME.getName(), context.getFunctionName());
        MDC.put(PowertoolsLoggedFields.FUNCTION_ARN.getName(), context.getInvokedFunctionArn());
        MDC.put(PowertoolsLoggedFields.FUNCTION_VERSION.getName(), context.getFunctionVersion());
        MDC.put(PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE.getName(),
                String.valueOf(context.getMemoryLimitInMB()));
        MDC.put(PowertoolsLoggedFields.FUNCTION_REQUEST_ID.getName(), context.getAwsRequestId());
        MDC.put(PowertoolsLoggedFields.FUNCTION_COLD_START.getName(), "false");
        MDC.put(PowertoolsLoggedFields.SAMPLING_RATE.getName(), "0.2");
        MDC.put(PowertoolsLoggedFields.SERVICE.getName(), "Service");

        // WHEN
        byte[] encoded = encoder.encode(loggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        assertThat(result).contains("{\"level\":\"INFO\",\"message\":\"message\",\"cold_start\":false,\"function_arn\":\"arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1\",\"function_memory_size\":1024,\"function_name\":\"testFunction\",\"function_request_id\":\"RequestId\",\"function_version\":1,\"sampling_rate\":0.2,\"service\":\"Service\",\"timestamp\":");

        // WHEN (powertoolsInfo = false)
        encoder.setIncludePowertoolsInfo(false);
        encoded = encoder.encode(loggingEvent);
        result = new String(encoded, StandardCharsets.UTF_8);

        // THEN (no powertools info in logs)
        assertThat(result).doesNotContain("cold_start", "function_arn", "function_memory_size", "function_name", "function_request_id", "function_version", "sampling_rate", "service");
    }

    @Test
    void shouldLogStructuredArgumentsAsNewEntries() {
        // GIVEN
        LambdaJsonEncoder encoder = new LambdaJsonEncoder();

        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("1212abcd");
        msg.setBody("plop");
        msg.setEventSource("eb");
        msg.setAwsRegion("eu-west-1");
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setStringListValues(Arrays.asList("val1", "val2", "val3"));
        msg.setMessageAttributes(Collections.singletonMap("keyAttribute", attribute));
        StructuredArgument argument = StructuredArguments.entry("msg", msg);

        // WHEN
        LoggingEvent loggingEvent = new LoggingEvent("fqcn", logger, Level.INFO, "A message", null, new Object[]{argument});
        byte[] encoded = encoder.encode(loggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN (logged as JSON)
        assertThat(result)
                .contains("\"message\":\"A message\",\"msg\":{\"awsRegion\":\"eu-west-1\",\"body\":\"plop\",\"eventSource\":\"eb\",\"messageAttributes\":{\"keyAttribute\":{\"stringListValues\":[\"val1\",\"val2\",\"val3\"]}},\"messageId\":\"1212abcd\"}");
    }

    @Test
    void shouldLogEventForHandlerWithLogEventAnnotation() {
        // GIVEN
        PowertoolsLogEvent requestHandler = new PowertoolsLogEvent();

        // WHEN
        requestHandler.handleRequest(singletonList("ListOfOneElement"), context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("\"event\":[\"ListOfOneElement\"]");
    }

    @Test
    void shouldLogEventForHandlerWhenEnvVariableSetToTrue() {
        try {
            // GIVEN
            LoggingConstants.POWERTOOLS_LOG_EVENT = true;

            PowertoolsLogEnabled requestHandler = new PowertoolsLogEnabled();

            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setBody("body");
            message.setMessageId("1234abcd");
            message.setAwsRegion("eu-west-1");

            // WHEN
            requestHandler.handleRequest(message, context);

            // THEN
            File logFile = new File("target/logfile.json");
            assertThat(contentOf(logFile))
                .contains("\"message\":\"Handler Event\"")
                .contains("\"event\":{\"awsRegion\":\"eu-west-1\",\"body\":\"body\",\"messageId\":\"1234abcd\"}");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_EVENT = false;
        }
    }

    @Test
    void shouldNotLogEventForHandlerWhenEnvVariableSetToFalse() throws IOException {
        // GIVEN
        LoggingConstants.POWERTOOLS_LOG_EVENT = false;

        // WHEN
        PowertoolsLogEventDisabled requestHandler = new PowertoolsLogEventDisabled();
        requestHandler.handleRequest(singletonList("ListOfOneElement"), context);

        // THEN
        Assertions.assertEquals(0,
                Files.lines(Paths.get("target/logfile.json")).collect(joining()).length());
    }

    @Test
    void shouldLogEventAsStringForStreamHandler() throws IOException {
        // GIVEN
        PowertoolsLogEventForStream requestStreamHandler = new PowertoolsLogEventForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(Collections.singletonMap("key", "value"))), output, context);

        // THEN
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isNotEmpty();

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains("\"message\":\"Handler Event\"")
                // logged as String for StreamHandler (should auto-escape double-quotes to avoid breaking JSON format)
                .contains("\"event\":\"{\\\"key\\\":\\\"value\\\"}\"");
    }

    @Test
    void shouldLogResponseForHandlerWithLogResponseAnnotation() {
        // GIVEN
        PowertoolsLogResponse requestHandler = new PowertoolsLogResponse();

        // WHEN
        requestHandler.handleRequest("input", context);

        // THEN
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains("\"message\":\"Handler Response\"")
                .contains("\"response\":\"Hola mundo\"");
    }

    @Test
    void shouldLogResponseForHandlerWhenEnvVariableSetToTrue() {
        try {
            // GIVEN
            LoggingConstants.POWERTOOLS_LOG_RESPONSE = true;

            PowertoolsLogEnabled requestHandler = new PowertoolsLogEnabled();

            // WHEN
            requestHandler.handleRequest("input", context);

            // THEN
            File logFile = new File("target/logfile.json");
            assertThat(contentOf(logFile))
                    .contains("\"message\":\"Handler Response\"")
                    .contains("\"response\":\"Bonjour le monde\"");
        } finally {
            LoggingConstants.POWERTOOLS_LOG_RESPONSE = false;
        }
    }

    @Test
    void shouldLogResponseForStreamHandler() throws IOException {
        // GIVEN
        PowertoolsLogResponseForStream requestStreamHandler = new PowertoolsLogResponseForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String input = "<user><firstName>Bob</firstName><lastName>The Sponge</lastName></user>";

        // WHEN
        requestStreamHandler.handleRequest(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, context);

        // THEN
        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isEqualTo(input);

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                    .contains("\"message\":\"Handler Response\"")
                    .contains("\"response\":\""+input+"\"");
    }

    @Test
    void shouldLogThreadInfo() {
        // GIVEN
        LambdaJsonEncoder encoder = new LambdaJsonEncoder();
        encoder.setIncludeThreadInfo(true);

        // WHEN
        byte[] encoded = encoder.encode(loggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        assertThat(result).contains("\"thread\":\"main\",\"thread_id\":1,\"thread_priority\":5");
    }

    @Test
    void shouldLogTimestampDifferently() {
        // GIVEN
        LambdaJsonEncoder encoder = new LambdaJsonEncoder();
        String pattern = "yyyy-MM-dd_HH";
        String timeZone = "Europe/Paris";
        encoder.setTimestampFormat(pattern);
        encoder.setTimestampFormatTimezoneId(timeZone);

        // WHEN
        Date date = new Date();
        byte[] encoded = encoder.encode(loggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        assertThat(result).contains("\"timestamp\":\""+simpleDateFormat.format(date)+"\"");
    }

    @Test
    void shouldLogException() {
        // GIVEN
        LambdaJsonEncoder encoder = new LambdaJsonEncoder();
        encoder.start();
        LoggingEvent errorloggingEvent = new LoggingEvent("fqcn", logger, Level.INFO, "Error", new IllegalStateException("Unexpected value"), null);

        // WHEN
        byte[] encoded = encoder.encode(errorloggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        assertThat(result).contains("\"message\":\"Error\",\"error\":{")
                .contains("\"message\":\"Unexpected value\"")
                .contains("\"name\":\"java.lang.IllegalStateException\"")
                .contains("\"stack\":\"[software.amazon.lambda.powertools.logging.internal.LambdaJsonEncoderTest.shouldLogException");

        // WHEN (configure a custom throwableConverter)
        encoder = new LambdaJsonEncoder();
        RootCauseFirstThrowableProxyConverter throwableConverter = new RootCauseFirstThrowableProxyConverter();
        encoder.setThrowableConverter(throwableConverter);
        encoder.start();
        encoded = encoder.encode(errorloggingEvent);
        result = new String(encoded, StandardCharsets.UTF_8);

        // THEN (stack is logged with root cause first)
        assertThat(result).contains("\"message\":\"Unexpected value\"")
                .contains("\"name\":\"java.lang.IllegalStateException\"")
                .contains("\"stack\":\"java.lang.IllegalStateException: Unexpected value\n");
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn(
                "arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(1024);
        when(context.getAwsRequestId()).thenReturn("RequestId");
    }
}
