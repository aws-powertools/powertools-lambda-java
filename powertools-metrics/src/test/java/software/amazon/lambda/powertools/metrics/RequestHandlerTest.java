package software.amazon.lambda.powertools.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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

import software.amazon.lambda.powertools.metrics.model.MetricUnit;

@ExtendWith(MockitoExtension.class)
class RequestHandlerTest {

    // For developer convenience, no exceptions should be thrown when using a plain Lambda Context mock
    @Mock
    Context lambdaContext;

    private final PrintStream standardOut = System.out;
    private ByteArrayOutputStream outputStreamCaptor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(standardOut);

        // Reset the singleton state between tests
        java.lang.reflect.Field field = MetricsFactory.class.getDeclaredField("metrics");
        field.setAccessible(true);
        field.set(null, null);

        field = MetricsFactory.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(null, new software.amazon.lambda.powertools.metrics.provider.EmfMetricsProvider());
    }

    @Test
    void shouldCaptureMetricsFromAnnotatedHandler() throws Exception {
        // Given
        RequestHandler<Map<String, Object>, String> handler = new HandlerWithMetricsAnnotation();
        Map<String, Object> input = new HashMap<>();

        // When
        handler.handleRequest(input, lambdaContext);

        // Then
        String emfOutput = outputStreamCaptor.toString().trim();
        JsonNode rootNode = objectMapper.readTree(emfOutput);

        assertThat(rootNode.has("test-metric")).isTrue();
        assertThat(rootNode.get("test-metric").asDouble()).isEqualTo(100.0);
        assertThat(rootNode.get("_aws").get("CloudWatchMetrics").get(0).get("Namespace").asText())
                .isEqualTo("CustomNamespace");
        assertThat(rootNode.has("Service")).isTrue();
        assertThat(rootNode.get("Service").asText()).isEqualTo("CustomService");
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
        @FlushMetrics(namespace = "CustomNamespace", service = "CustomService")
        public String handleRequest(Map<String, Object> input, Context context) {
            Metrics metrics = MetricsFactory.getMetricsInstance();
            metrics.addMetric("test-metric", 100, MetricUnit.COUNT);
            return "OK";
        }
    }
}
