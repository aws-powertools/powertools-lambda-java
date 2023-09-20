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

package org.apache.logging.log4j.layout.template.json.resolver;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled;

class PowerToolsResolverFactoryTest {

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
            FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // file may not exist on the first launch
        }
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "testLog4j")
    @SetEnvironmentVariable(key = "POWERTOOLS_LOGGER_SAMPLE_RATE", value = "0.000000001")
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "Root=1-63441c4a-abcdef012345678912345678")
    void shouldLogInJsonFormat() {
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
        handler.handleRequest("Input", context);

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).startsWith(
                        "{\"cold_start\":true,\"function_arn\":\"arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1\",\"function_memory_size\":1024,\"function_name\":\"testFunction\",\"function_request_id\":\"RequestId\",\"function_version\":\"1\",\"level\":\"INFO\",\"message\":\"Test event\",\"sampling_rate\":1.0E-9,\"service\":\"testLog4j\",\"timestamp\":")
                .endsWith("\"xray_trace_id\":\"1-63441c4a-abcdef012345678912345678\",\"myKey\":\"myValue\"}\n");
    }

    @Test
    @SetEnvironmentVariable(key = "AWS_REGION", value = "eu-central-1")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "testLog4jEcs")
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "Root=1-63441c4a-abcdef012345678912345678")
    void shouldLogInEcsFormat() {
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
        handler.handleRequest("Input", context);

        File logFile = new File("target/ecslogfile.json");
        assertThat(contentOf(logFile)).endsWith(
                        "\"ecs.version\":\"1.2.0\",\"log.level\":\"INFO\",\"message\":\"Test event\",\"service.name\":\"testLog4j\",\"service.version\":\"1\",\"process.thread.name\":\"main\",\"log.logger\":\"software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled\",\"cloud.provider\":\"aws\",\"cloud.service.name\":\"lambda\",\"cloud.region\":\"eu-central-1\",\"cloud.account.id\":\"012345678910\",\"faas.coldstart\":true,\"faas.id\":\"arn:aws:lambda:eu-west-1:012345678910:function:testFunction:1\",\"faas.memory\":1024,\"faas.name\":\"testFunction\",\"faas.execution\":\"RequestId\",\"faas.version\":\"1\",\"myKey\":\"myValue\",\"trace.id\":\"1-63441c4a-abcdef012345678912345678\"}\n")
                .startsWith("{\"@timestamp\":\"");
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
