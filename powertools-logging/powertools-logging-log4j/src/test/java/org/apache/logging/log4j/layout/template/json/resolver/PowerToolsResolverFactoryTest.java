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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.stubs.TestLambdaContext;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled;

@Order(1)
class PowerToolsResolverFactoryTest {

    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException, IOException {
        MDC.clear();
        // Reset cold start state
        writeStaticField(LambdaHandlerProcessor.class, "isColdStart", null, true);
        writeStaticField(PowertoolsLogging.class, "hasBeenInitialized", false, true);

        context = new TestLambdaContext();
        // Make sure file is cleaned up before running tests
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
            FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // file may not exist on the first launch
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        FileChannel.open(Paths.get("target/ecslogfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldLogInJsonFormat() {
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
        handler.handleRequest("Input", context);

        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains(
                "{\"level\":\"DEBUG\",\"message\":\"Test debug event\",\"cold_start\":true,\"function_arn\":\"arn:aws:lambda:us-east-1:123456789012:function:test\",\"function_memory_size\":128,\"function_name\":\"test-function\",\"function_request_id\":\"test-request-id\",\"function_version\":\"1\",\"service\":\"testLog4j\",\"timestamp\":")
                .contains("\"xray_trace_id\":\"1-63441c4a-abcdef012345678912345678\",\"myKey\":\"myValue\"}\n");
    }

    @Test
    void shouldLogInEcsFormat() {
        PowertoolsLogEnabled handler = new PowertoolsLogEnabled();
        handler.handleRequest("Input", context);

        File logFile = new File("target/ecslogfile.json");
        assertThat(contentOf(logFile)).contains(
                "\"ecs.version\":\"1.2.0\",\"log.level\":\"DEBUG\",\"message\":\"Test debug event\",\"service.name\":\"testLog4j\",\"service.version\":\"1\",\"log.logger\":\"software.amazon.lambda.powertools.logging.internal.handler.PowertoolsLogEnabled\",\"process.thread.name\":\"main\",\"cloud.provider\":\"aws\",\"cloud.service.name\":\"lambda\",\"cloud.region\":\"eu-central-1\",\"cloud.account.id\":\"123456789012\",\"faas.id\":\"arn:aws:lambda:us-east-1:123456789012:function:test\",\"faas.name\":\"test-function\",\"faas.version\":\"1\",\"faas.memory\":128,\"faas.execution\":\"test-request-id\",\"faas.coldstart\":true,\"trace.id\":\"1-63441c4a-abcdef012345678912345678\",\"myKey\":\"myValue\"}\n");
    }

}
