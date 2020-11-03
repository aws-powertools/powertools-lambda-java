package software.amazon.lambda.powertools.metrics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class MetricsLoggerTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @BeforeAll
    static void beforeAll() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
        }
    }

    @Test
    void singleMetricsCaptureUtility() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");

            MetricsUtils.withSingleMetric("Metric1", 1, Unit.COUNT, "test",
                    metricsLogger -> metricsLogger.setDimensions(DimensionSet.of("Dimension1", "Value1")));

            assertThat(out.toString())
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Dimension1", "Value1")
                                .containsKey("_aws");
                    });
        }
    }

    private Map<String, Object> readAsJson(String s) {
        try {
            return mapper.readValue(s, Map.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return emptyMap();
    }
}