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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.logging.LambdaEcsEncoder;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled;

@Order(3)
class LambdaEcsEncoderTest {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(LambdaEcsEncoderTest.class.getName());


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
            FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // file may not exist on the first launch
        }
    }

    @AfterEach
    void cleanUp() throws IOException{
//        FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldLogInEcsFormat() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> getenv("_X_AMZN_TRACE_ID"))
                    .thenReturn("Root=1-63441c4a-abcdef012345678912345678");
            mocked.when(() -> getenv("AWS_REGION"))
                    .thenReturn("eu-central-1");

            PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
            handler.handleRequest("Input", context);

            File logFile = new File("target/ecslogfile.json");
            assertThat(contentOf(logFile)).contains(
                    "\"ecs.version\":\"1.2.0\",\"log.level\":\"DEBUG\",\"message\":\"Test debug event\",\"service.name\":\"testLogback\",\"service.version\":\"1\",\"log.logger\":\"software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled\",\"process.thread.name\":\"main\",\"cloud.provider\":\"aws\",\"cloud.service.name\":\"lambda\",\"cloud.region\":\"eu-west-1\",\"cloud.account.id\":\"012345678910\",\"faas.id\":\"arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1\",\"faas.name\":\"testFunction\",\"faas.version\":\"1\",\"faas.memory\":\"1024\",\"faas.execution\":\"RequestId\",\"faas.coldstart\":\"true\",\"trace.id\":\"1-63441c4a-abcdef012345678912345678\",\"myKey\":\"myValue\"}\n");
        }
    }

    private final LoggingEvent loggingEvent = new LoggingEvent("fqcn", logger, Level.INFO, "message", null, null);

    @Test
    void shouldNotLogFunctionInfo() {
        // GIVEN
        LambdaEcsEncoder encoder = new LambdaEcsEncoder();
        setMDC();

        // WHEN
        byte[] encoded = encoder.encode(loggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        assertThat(result).contains("\"faas.id\":\"arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1\",\"faas.name\":\"testFunction\",\"faas.version\":\"1\",\"faas.memory\":\"1024\",\"faas.execution\":\"RequestId\",\"faas.coldstart\":\"false\"");

        // WHEN (includeFaasInfo = false)
        encoder.setIncludeFaasInfo(false);
        encoded = encoder.encode(loggingEvent);
        result = new String(encoded, StandardCharsets.UTF_8);

        // THEN (no faas info in logs)
        assertThat(result).doesNotContain("faas");
    }

    @Test
    void shouldNotLogCloudInfo() {
        // GIVEN
        LambdaEcsEncoder encoder = new LambdaEcsEncoder();
        setMDC();

        // WHEN
        byte[] encoded = encoder.encode(loggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        assertThat(result).contains("\"cloud.provider\":\"aws\",\"cloud.service.name\":\"lambda\",\"cloud.region\":\"eu-west-1\",\"cloud.account.id\":\"012345678910\"");

        // WHEN (includeCloudInfo = false)
        encoder.setIncludeCloudInfo(false);
        encoded = encoder.encode(loggingEvent);
        result = new String(encoded, StandardCharsets.UTF_8);

        // THEN (no faas info in logs)
        assertThat(result).doesNotContain("cloud");
    }

    @Test
    void shouldLogException() {
        // GIVEN
        LambdaEcsEncoder encoder = new LambdaEcsEncoder();
        encoder.start();
        LoggingEvent errorloggingEvent = new LoggingEvent("fqcn", logger, Level.INFO, "Error", new IllegalStateException("Unexpected value"), null);

        // WHEN
        byte[] encoded = encoder.encode(errorloggingEvent);
        String result = new String(encoded, StandardCharsets.UTF_8);

        // THEN
        assertThat(result).contains("\"message\":\"Error\",\"error.message\":\"Unexpected value\",\"error.type\":\"java.lang.IllegalStateException\",\"error.stack_trace\":\"[software.amazon.lambda.powertools.logging.internal.LambdaEcsEncoderTest.shouldLogException");

        // WHEN (configure a custom throwableConverter)
        encoder = new LambdaEcsEncoder();
        RootCauseFirstThrowableProxyConverter throwableConverter = new RootCauseFirstThrowableProxyConverter();
        encoder.setThrowableConverter(throwableConverter);
        encoder.start();
        encoded = encoder.encode(errorloggingEvent);
        result = new String(encoded, StandardCharsets.UTF_8);

        // THEN (stack is logged with root cause first)
        assertThat(result).contains("\"message\":\"Error\",\"error.message\":\"Unexpected value\",\"error.type\":\"java.lang.IllegalStateException\",\"error.stack_trace\":\"java.lang.IllegalStateException: Unexpected value\n");
    }

    private void setMDC() {
        MDC.put(PowertoolsLoggedFields.FUNCTION_NAME.getName(), context.getFunctionName());
        MDC.put(PowertoolsLoggedFields.FUNCTION_ARN.getName(), context.getInvokedFunctionArn());
        MDC.put(PowertoolsLoggedFields.FUNCTION_VERSION.getName(), context.getFunctionVersion());
        MDC.put(PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE.getName(),
                String.valueOf(context.getMemoryLimitInMB()));
        MDC.put(PowertoolsLoggedFields.FUNCTION_REQUEST_ID.getName(), context.getAwsRequestId());
        MDC.put(PowertoolsLoggedFields.FUNCTION_COLD_START.getName(), "false");
        MDC.put(PowertoolsLoggedFields.SAMPLING_RATE.getName(), "0.2");
        MDC.put(PowertoolsLoggedFields.SERVICE.getName(), "Service");
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
