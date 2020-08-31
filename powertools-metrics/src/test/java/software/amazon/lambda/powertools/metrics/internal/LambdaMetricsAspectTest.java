package software.amazon.lambda.powertools.metrics.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledHandler;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.amazon.cloudwatchlogs.emf.model.Unit.BYTES;

public class LambdaMetricsAspectTest {

    @Mock
    private Context context;

    @Spy
    private MetricsLogger logger;

    private RequestHandler<Object, Object> requestHandler;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        initMocks(this);
        setupContext();
        writeStaticField(PowertoolsMetricsLogger.class, "metricsLogger", logger, true);
        requestHandler = new PowertoolsMetricsEnabledHandler();
    }

    @Test
    public void testHandler() {
        requestHandler.handleRequest("input", context);

        MetricsLogger logger = PowertoolsMetricsLogger.logger();

        verify(logger).setDimensions(any(DimensionSet.class));
        verify(logger).setNamespace("ExampleApplication");
        verify(logger).putMetric("Metric1", 1, BYTES);
        verify(logger).flush();
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}
