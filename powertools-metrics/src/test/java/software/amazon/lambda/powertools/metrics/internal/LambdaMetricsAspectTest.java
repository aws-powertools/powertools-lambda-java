package software.amazon.lambda.powertools.metrics.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.handlers.PowertoolsMetricsEnabledHandler;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class LambdaMetricsAspectTest {

    @Mock
    private Context context;

    private RequestHandler<Object, Object> requestHandler;

    @BeforeEach
    void setUp() {
        initMocks(this);
        setupContext();
        requestHandler = new PowertoolsMetricsEnabledHandler();
    }

    @Test
    public void testHandler() {
        MetricsLogger metricsLogger = new MetricsLogger();
        metricsLogger.flush();

        requestHandler.handleRequest("input", context);
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}
