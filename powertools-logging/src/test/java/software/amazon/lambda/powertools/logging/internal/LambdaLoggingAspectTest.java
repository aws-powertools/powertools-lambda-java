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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.ThreadContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.handlers.PowerLogToolEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowerLogToolEnabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowerToolDisabled;
import software.amazon.lambda.powertools.logging.handlers.PowerToolDisabledForStream;
import software.amazon.lambda.powertools.logging.handlers.PowerToolLogEventEnabled;
import software.amazon.lambda.powertools.logging.handlers.PowerToolLogEventEnabledForStream;

class LambdaLoggingAspectTest {

    private static final int EXPECTED_CONTEXT_SIZE = 6;
    private RequestStreamHandler requestStreamHandler;
    private RequestHandler<Object, Object> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        ThreadContext.clearAll();
        FieldUtils.writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        requestHandler = new PowerLogToolEnabled();
        requestStreamHandler = new PowerLogToolEnabledForStream();
    }

    @Test
    void shouldSetLambdaContextWhenEnabled() {
        requestHandler.handleRequest(new Object(), context);

        Assertions.assertThat(ThreadContext.getImmutableContext())
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

        Assertions.assertThat(ThreadContext.getImmutableContext())
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

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .hasSize(6)
                .containsEntry("coldStart", "true");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), context);

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry("coldStart", "false");
    }

    @Test
    void shouldNotSetLambdaContextWhenDisabled() {
        requestHandler = new PowerToolDisabled();

        requestHandler.handleRequest(new Object(), context);

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldNotSetLambdaContextForStreamHandlerWhenDisabled() throws IOException {
        requestStreamHandler = new PowerToolDisabledForStream();

        requestStreamHandler.handleRequest(null, null, context);

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldHaveNoEffectIfNotUsedOnLambdaHandler() {
        PowerLogToolEnabled handler = new PowerLogToolEnabled();

        handler.anotherMethod();

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldLogEventForHandler() {
        requestHandler = new PowerToolLogEventEnabled();

        requestHandler.handleRequest(new Object(), context);

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE);
    }

    @Test
    void shouldLogEventForStreamAndLambdaStreamIsValid() throws IOException {
        requestStreamHandler = new PowerToolLogEventEnabledForStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Map<String, String> testPayload = new HashMap<>();
        testPayload.put("test", "payload");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(testPayload)), output, context);

        Assertions.assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .isEqualTo("{\"test\":\"payload\"}");

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE);
    }

    @Test
    void shouldLogServiceNameWhenEnvVarSet() throws IllegalAccessException {
        FieldUtils.writeStaticField(LambdaHandlerProcessor.class, "SERVICE_NAME", "testService", true);
        requestHandler.handleRequest(new Object(), context);

        Assertions.assertThat(ThreadContext.getImmutableContext())
                .hasSize(EXPECTED_CONTEXT_SIZE)
                .containsEntry("service", "testService");
    }

    private void setupContext() {
        Mockito.when(context.getFunctionName()).thenReturn("testFunction");
        Mockito.when(context.getInvokedFunctionArn()).thenReturn("testArn");
        Mockito.when(context.getFunctionVersion()).thenReturn("1");
        Mockito.when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}