package software.aws.lambda.logging;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LambdaAspectTest {

    private RequestStreamHandler requestStreamHandler;
    private RequestHandler<Object, Object> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        initMocks(this);
        ThreadContext.clearAll();
        LambdaAspect.IS_COLD_START = null;
        setupContext();
        requestHandler = new PowerLogToolEnabled();
        requestStreamHandler = new PowerLogToolEnabledForStream();
    }

    @Test
    void shouldSetLambdaContextWhenEnabled() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(5)
                .containsEntry(DefaultLambdaFields.FUNCTION_ARN.getName(), "testArn")
                .containsEntry(DefaultLambdaFields.FUNCTION_MEMORY_SIZE.getName(), "10")
                .containsEntry(DefaultLambdaFields.FUNCTION_VERSION.getName(), "1")
                .containsEntry(DefaultLambdaFields.FUNCTION_NAME.getName(), "testFunction")
                .containsKey("coldStart");
    }

    @Test
    void shouldSetLambdaContextForStreamHandlerWhenEnabled() throws IOException {
        requestStreamHandler = new PowerLogToolEnabledForStream();

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), null, context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(5)
                .containsEntry(DefaultLambdaFields.FUNCTION_ARN.getName(), "testArn")
                .containsEntry(DefaultLambdaFields.FUNCTION_MEMORY_SIZE.getName(), "10")
                .containsEntry(DefaultLambdaFields.FUNCTION_VERSION.getName(), "1")
                .containsEntry(DefaultLambdaFields.FUNCTION_NAME.getName(), "testFunction")
                .containsKey("coldStart");
    }

    @Test
    void shouldSetColdStartFlag() throws IOException {
        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), null, context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(5)
                .containsEntry("coldStart", "true");

        requestStreamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), null, context);

        assertThat(ThreadContext.getImmutableContext())
                .hasSize(5)
                .containsEntry("coldStart", "false");
    }

    @Test
    void shouldNotSetLambdaContextWhenDisabled() {
        requestHandler = new PowerLogToolDisabled();

        requestHandler.handleRequest(new Object(), context);

        assertThat(ThreadContext.getImmutableContext())
                .isEmpty();
    }

    @Test
    void shouldNotSetLambdaContextForStreamHandlerWhenDisabled() throws IOException {
        requestStreamHandler = new PowerLogToolDisabledForStream();

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
                .hasSize(5);
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
                .hasSize(5);
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}