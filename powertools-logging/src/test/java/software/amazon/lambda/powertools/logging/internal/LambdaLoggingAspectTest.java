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
package software.amazon.lambda.powertools.logging.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.handlers.PowerLogToolEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowerLogToolEnabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowerToolDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowerToolDisabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowerToolLogEventEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowerToolLogEventEnabledForStream;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LambdaLoggingAspectTest {

    private static final int EXPECTED_CONTEXT_SIZE = 7;
    private RequestStreamHandler requestStreamHandler;
    private RequestHandler<Object, Object> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        initMocks(this);
        ThreadContext.clearAll();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        requestHandler = new PowerLogToolEnabled();
        requestStreamHandler = new PowerLogToolEnabledForStream();
    }

    @Test
    void shouldSetLambdaContextWhenEnabled() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(DefaultLambdaFields.FUNCTION_ARN.getName(), "testArn")
                .containsEntry(DefaultLambdaFields.FUNCTION_MEMORY_SIZE.getName(), "10")
                .containsEntry(DefaultLambdaFields.FUNCTION_VERSION.getName(), "1")
                .containsEntry(DefaultLambdaFields.FUNCTION_NAME.getName(), "testFunction")
                .containsKey("coldStart")
                .containsKey("service");
    }

    @Test
    void shouldSetLambdaContextForStreamHandlerWhenEnabled() throws IOException {
        requestStreamHandler = new PowerLogToolEnabledForStream();

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry(DefaultLambdaFields.FUNCTION_ARN.getName(), "testArn")
                .containsEntry(DefaultLambdaFields.FUNCTION_MEMORY_SIZE.getName(), "10")
                .containsEntry(DefaultLambdaFields.FUNCTION_VERSION.getName(), "1")
                .containsEntry(DefaultLambdaFields.FUNCTION_NAME.getName(), "testFunction")
                .containsKey("coldStart")
                .containsKey("service");
    }

    @Test
    void shouldSetColdStartFlag() throws IOException {
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry("coldStart", "true");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry("coldStart", "false");
    }

    @Test
    void shouldNotSetLambdaContextWhenDisabled() {
        requestHandler = new PowerToolDisabled();

        requestHandler.handleRequest(new Object(), context);

        assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldNotSetLambdaContextForStreamHandlerWhenDisabled() throws IOException {
        requestStreamHandler = new PowerToolDisabledForStream();

        requestStreamHandler.handleRequest(null, null, context);

        assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldHaveNoEffectIfNotUsedOnLambdaHandler() {
        PowerLogToolEnabled handler = new PowerLogToolEnabled();

        handler.anotherMethod();

        assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldLogEventForHandler() {
        requestHandler = new PowerToolLogEventEnabled();

        requestHandler.handleRequest(new Object(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE);
    }

    @Test
    void shouldLogEventForStreamAndLambdaStreamIsValid() throws IOException {
        requestStreamHandler = new PowerToolLogEventEnabledForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Map<String, String> testPayload = new HashMap<>();
        testPayload.put("test", "payload");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(testPayload)), output, context);

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isEqualTo("{\"test\":\"payload\"}");

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE);
    }

    @Test
    void shouldLogServiceNameWhenEnvVarSet() throws IllegalAccessException {
        writeStaticField(LambdaHandlerProcessor.class, "SERVICE_NAME", "testService", true);
        requestHandler.handleRequest(new Object(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry("service", "testService");
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}