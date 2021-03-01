package software.amazon.lambda.powertools.metrics.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.ValidationException;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsColdStartEnabledHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledStreamHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsExceptionWhenNoMetricsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsNoDimensionsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsNoExceptionWhenNoMetricsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsTooManyDimensionsHandler;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsWithExceptionInHandler;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.core.internal.SystemWrapper.getenv;

public class LambdaMetricsAspectTest {
    @Mock
    private Context context;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final ObjectMapper mapper = new ObjectMapper();
    private RequestHandler<Object, Object> requestHandler;
    private RequestStreamHandler streamHandler;


    @BeforeAll
    static void beforeAll() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
        }
    }

    @BeforeEach
    void setUp() throws IllegalAccessException {
        openMocks(this);
        setupContext();
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        System.setOut(new PrintStream(out));
        requestHandler = new PowertoolsMetricsEnabledHandler();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void metricsWithoutColdStart() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class);
             MockedStatic<software.amazon.lambda.powertools.core.internal.SystemWrapper> internalWrapper = mockStatic(software.amazon.lambda.powertools.core.internal.SystemWrapper.class)) {

            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            internalWrapper.when(() -> getenv("_X_AMZN_TRACE_ID")).thenReturn("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1\"");

            requestHandler.handleRequest("input", context);

            assertThat(out.toString().split("\n"))
                    .hasSize(2)
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s[0]);

                        assertThat(logAsJson)
                                .containsEntry("Metric2", 1.0)
                                .containsEntry("Dimension1", "Value1")
                                .containsKey("_aws")
                                .containsEntry("XrayTraceId", "1-5759e988-bd862e3fe1be46a994272793")
                                .containsEntry("AwsRequestId", "123ABC");

                        Map<String, Object> aws = (Map<String, Object>) logAsJson.get("_aws");

                        assertThat(aws.get("CloudWatchMetrics"))
                                .asString()
                                .contains("Namespace=ExampleApplication");

                        logAsJson = readAsJson(s[1]);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");
                    });
        }
    }

    @Test
    public void metricsWithColdStart() {
        requestHandler = new PowertoolsMetricsColdStartEnabledHandler();

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {

            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            requestHandler.handleRequest("input", context);

            assertThat(out.toString().split("\n"))
                    .hasSize(2)
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s[0]);

                        assertThat(logAsJson)
                                .doesNotContainKey("Metric1")
                                .containsEntry("ColdStart", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");

                        logAsJson = readAsJson(s[1]);

                        assertThat(logAsJson)
                                .doesNotContainKey("ColdStart")
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");
                    });
        }
    }

    @Test
    public void noColdStartMetricsWhenColdStartDone() {
        requestHandler = new PowertoolsMetricsColdStartEnabledHandler();

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            requestHandler.handleRequest("input", context);
            requestHandler.handleRequest("input", context);

            assertThat(out.toString().split("\n"))
                    .hasSize(3)
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s[0]);

                        assertThat(logAsJson)
                                .doesNotContainKey("Metric1")
                                .containsEntry("ColdStart", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");

                        logAsJson = readAsJson(s[1]);

                        assertThat(logAsJson)
                                .doesNotContainKey("ColdStart")
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");

                        logAsJson = readAsJson(s[2]);

                        assertThat(logAsJson)
                                .doesNotContainKey("ColdStart")
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");
                    });
        }
    }

    @Test
    public void metricsWithStreamHandler() throws IOException {
        streamHandler = new PowertoolsMetricsEnabledStreamHandler();

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");

            streamHandler.handleRequest(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), context);

            assertThat(out.toString())
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Metric1", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");
                    });
        }
    }

    @Test
    public void exceptionWhenNoMetricsEmitted() {
        requestHandler = new PowertoolsMetricsExceptionWhenNoMetricsHandler();
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");

            assertThatExceptionOfType(ValidationException.class)
                    .isThrownBy(() -> requestHandler.handleRequest("input", context))
                    .withMessage("No metrics captured, at least one metrics must be emitted");
        }
    }

    @Test
    public void noExceptionWhenNoMetricsEmitted() {
        requestHandler = new PowertoolsMetricsNoExceptionWhenNoMetricsHandler();

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            requestHandler.handleRequest("input", context);

            assertThat(out.toString())
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s);

                        assertThat(logAsJson)
                                .containsEntry("Service", "booking")
                                .doesNotContainKey("_aws");
                    });
        }
    }

    @Test
    public void exceptionWhenNoDimensionsSet() {
        requestHandler = new PowertoolsMetricsNoDimensionsHandler();
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");

            assertThatExceptionOfType(ValidationException.class)
                    .isThrownBy(() -> requestHandler.handleRequest("input", context))
                    .withMessage("Number of Dimensions must be in range of 1-9. Actual size: 0.");
        }
    }

    @Test
    public void exceptionWhenTooManyDimensionsSet() {
        requestHandler = new PowertoolsMetricsTooManyDimensionsHandler();

        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");

            assertThatExceptionOfType(ValidationException.class)
                    .isThrownBy(() -> requestHandler.handleRequest("input", context))
                    .withMessage("Number of Dimensions must be in range of 1-9. Actual size: 14.");
        }
    }

    @Test
    public void metricsPublishedEvenHandlerThrowsException() {
        requestHandler = new PowertoolsMetricsWithExceptionInHandler();
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> SystemWrapper.getenv("AWS_EMF_ENVIRONMENT")).thenReturn("Lambda");
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> requestHandler.handleRequest("input", context))
                    .withMessage("Whoops, unexpected exception");

            assertThat(out.toString())
                    .satisfies(s -> {
                        Map<String, Object> logAsJson = readAsJson(s);
                        assertThat(logAsJson)
                                .containsEntry("CoolMetric", 1.0)
                                .containsEntry("Service", "booking")
                                .containsEntry("AwsRequestId", "123ABC")
                                .containsKey("_aws");
                    });
        }
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
        when(context.getAwsRequestId()).thenReturn("123ABC");
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
