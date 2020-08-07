package software.aws.lambda.tracing.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.xray.AWSXRay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.aws.lambda.handlers.PowerTracerToolEnabled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LambdaTracingAspectTest {
    private RequestHandler<Object, Object> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        initMocks(this);
        setupContext();
        requestHandler = new PowerTracerToolEnabled();
        AWSXRay.beginSegment(LambdaTracingAspectTest.class.getName());
    }

    @AfterEach
    void tearDown() {
        AWSXRay.endSegment();
    }

    @Test
    void shouldCaptureTraces() {

        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity().getSubsegments())
                .hasSize(1)
                .allSatisfy(subsegment -> {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(1)
                            .containsEntry("ColdStart", true);

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("service_undefined");
                });
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}