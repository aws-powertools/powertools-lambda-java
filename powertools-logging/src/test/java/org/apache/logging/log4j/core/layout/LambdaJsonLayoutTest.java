/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.lambda.powertools.logging.handlers.PowerLogToolEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowerLogToolSamplingEnabled;
import software.amazon.lambda.powertools.logging.internal.LambdaLoggingAspect;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LambdaJsonLayoutTest {

    private RequestHandler<Object, Object> handler = new PowerLogToolEnabled();

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        initMocks(this);
        setupContext();
        //Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        resetLogLevel(Level.INFO);
    }

    @Test
    void shouldLogInStructuredFormat() throws IOException {
        handler.handleRequest("test", context);

        assertThat(Files.lines(Paths.get("target/logfile.json")))
                .hasSize(1)
                .allSatisfy(line -> assertThat(parseToMap(line))
                        .containsEntry("functionName", "testFunction")
                        .containsEntry("functionVersion", "1")
                        .containsEntry("functionMemorySize", "10")
                        .containsEntry("functionArn", "testArn")
                        .containsKey("timestamp")
                        .containsKey("message")
                        .containsKey("service"));
    }

    @Test
    void shouldModifyLogLevelBasedOnEnvVariable() throws IllegalAccessException, IOException, NoSuchMethodException, InvocationTargetException {
        resetLogLevel(Level.DEBUG);

        handler.handleRequest("test", context);

        assertThat(Files.lines(Paths.get("target/logfile.json")))
                .hasSize(2)
                .satisfies(line -> {
                    assertThat(parseToMap(line.get(0)))
                            .containsEntry("level", "INFO")
                            .containsEntry("message", "Test event");

                    assertThat(parseToMap(line.get(1)))
                            .containsEntry("level", "DEBUG")
                            .containsEntry("message", "Test debug event");
                });
    }

    @Test
    void shouldModifyLogLevelBasedOnSamplingRule() throws IOException {
        handler = new PowerLogToolSamplingEnabled();

        handler.handleRequest("test", context);

        assertThat(Files.lines(Paths.get("target/logfile.json")))
                .hasSize(3)
                .satisfies(line -> {
                    assertThat(parseToMap(line.get(0)))
                            .containsEntry("level", "DEBUG")
                            .containsEntry("loggerName", LambdaLoggingAspect.class.getCanonicalName());

                    assertThat(parseToMap(line.get(1)))
                            .containsEntry("level", "INFO")
                            .containsEntry("message", "Test event");

                    assertThat(parseToMap(line.get(2)))
                            .containsEntry("level", "DEBUG")
                            .containsEntry("message", "Test debug event");
                });
    }

    private void resetLogLevel(Level level) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method resetLogLevels = LambdaLoggingAspect.class.getDeclaredMethod("resetLogLevels", Level.class);
        resetLogLevels.setAccessible(true);
        resetLogLevels.invoke(null, level);
        writeStaticField(LambdaLoggingAspect.class, "LEVEL_AT_INITIALISATION", level, true);
    }

    private Map<String, Object> parseToMap(String stringAsJson) {
        try {
            return new ObjectMapper().readValue(stringAsJson, Map.class);
        } catch (JsonProcessingException e) {
            fail("Failed parsing logger line " + stringAsJson);
            return emptyMap();
        }
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}