package software.amazon.lambda.powertools.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

@ExtendWith(MockitoExtension.class)
class RequestHandlerTest {

    // For developer convenience, no exceptions should be thrown when using a plain Lambda Context mock
    @Mock
    Context lambdaContext;

    private static final PrintStream STDOUT = System.out;
    private ByteArrayOutputStream outputStreamCaptor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // Reset LambdaHandlerProcessor's serviceName
        Method resetServiceName = LambdaHandlerProcessor.class.getDeclaredMethod("resetServiceName");
        resetServiceName.setAccessible(true);
        resetServiceName.invoke(null);

        // Reset isColdStart
        java.lang.reflect.Field coldStartField = LambdaHandlerProcessor.class.getDeclaredField("isColdStart");
        coldStartField.setAccessible(true);
        coldStartField.set(null, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(STDOUT);

        // Reset the singleton state between tests
        java.lang.reflect.Field field = MetricsFactory.class.getDeclaredField("metricsProxy");
        field.setAccessible(true);
        field.set(null, null);

        field = MetricsFactory.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(null, new software.amazon.lambda.powertools.metrics.provider.EmfMetricsProvider());
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_NAMESPACE", value = "TestNamespace")
    @SetEnvironmentVariable(key = "POWERTOOLS_SERVICE_NAME", value = "TestService")
    void shouldCaptureMetricsFromAnnotatedHandler() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithMetricsAnnotation();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, lambdaContext);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        String[] jsonLines = emfOutput.split("\n");

        // First JSON object should be the cold start metric
        JsonNode coldStartNode = objectMapper.readTree(jsonLines[0]);
        assertThat(coldStartNode.has("ColdStart")).isTrue();
        assertThat(coldStartNode.get("ColdStart").asDouble()).isEqualTo(1.0);
        assertThat(coldStartNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("TestNamespace");
        assertThat(coldStartNode.has("Service")).isTrue();
        assertThat(coldStartNode.get("Service").asText()).isEqualTo("TestService");

        // Second JSON object should be the regular metric
        JsonNode regularNode = objectMapper.readTree(jsonLines[1]);
        assertThat(regularNode.has("test-metric")).isTrue();
        assertThat(regularNode.get("test-metric").asDouble()).isEqualTo(100.0);
        assertThat(regularNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("TestNamespace");
        assertThat(regularNode.has("Service")).isTrue();
        assertThat(regularNode.get("Service").asText()).isEqualTo("TestService");
    }

    @SetEnvironmentVariable(key = "POWERTOOLS_METRICS_DISABLED", value = "true")
    @Test
    void shouldNotCaptureMetricsWhenDisabled() {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithMetricsAnnotation();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, lambdaContext);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        assertThat(emfOutput).isEmpty();
    }

    static class HandlerWithMetricsAnnotation implements RequestHandler<Map<String, Object>, String> {
        @Override
        @FlushMetrics(captureColdStart = true)
        public String handleRequest(Map<String, Object> input, Context context) {
            Metrics metrics = MetricsFactory.getMetricsInstance();
            metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }
}
