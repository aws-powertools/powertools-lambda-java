package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.tracing.opentelemetry.CaptureMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.TracingOpenTelemetry;
import software.amazon.lambda.powertools.tracing.opentelemetry.TracingOtel;

class TracingOpenTelemetryAspectTest {

    private ProceedingJoinPoint pjp;
    private TracingOtel tracingOtel;
    private TracingOpenTelemetry tracingOpenTelemetry;
    private SpanScope spanScope;
    private Signature signature;
    private TracingOpenTelemetry originalTracing;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        pjp = mock(ProceedingJoinPoint.class);
        tracingOtel = mock(TracingOtel.class);
        tracingOpenTelemetry = mock(TracingOpenTelemetry.class);
        spanScope = mock(SpanScope.class);
        signature = mock(Signature.class);

        originalTracing = (TracingOpenTelemetry) FieldUtils
                .readStaticField(TracingOpenTelemetryAspect.class, "tracing", true);

        FieldUtils.writeStaticField(TracingOpenTelemetryAspect.class, "tracing", tracingOpenTelemetry, true);
    }

    @AfterEach
    void tearDown() throws IllegalAccessException {
        FieldUtils.writeStaticField(TracingOpenTelemetryAspect.class, "tracing", originalTracing, true);
    }

    @Test
    void testAroundMethodSuccessfulExecution() throws Throwable {

        when(tracingOpenTelemetry.addSpan(anyString())).thenReturn(spanScope);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(signature.getDeclaringType()).thenReturn(RequestHandler.class);
        Object[] args = new Object[0];
        when(pjp.getArgs()).thenReturn(args);
        when(tracingOtel.spanName()).thenReturn("testMethod");
        when(tracingOtel.namespace()).thenReturn("test");
        when(tracingOtel.captureMode()).thenReturn(CaptureMode.ENVIRONMENT_VAR);
        when(pjp.proceed(any(Object[].class))).thenReturn("Success");

        TracingOpenTelemetryAspect aspect = new TracingOpenTelemetryAspect();
        Object result = aspect.around(pjp, tracingOtel);

        verify(tracingOpenTelemetry).addSpan("testMethod");
        verify(pjp).proceed(any(Object[].class));
        assertEquals("Success", result);
    }

    @Test
    void testAroundMethodExceptionFlow() throws Throwable {


        when(tracingOpenTelemetry.addSpan(anyString())).thenReturn(spanScope);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(signature.getDeclaringType()).thenReturn(RequestHandler.class);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        Throwable mockThrowable = new RuntimeException("Test Exception");
        when(tracingOtel.spanName()).thenReturn("testMethod");
        when(tracingOtel.namespace()).thenReturn("test");
        when(tracingOtel.captureMode()).thenReturn(CaptureMode.ERROR);
        when(pjp.proceed(pjp.getArgs())).thenThrow(mockThrowable);

        TracingOpenTelemetryAspect aspect = new TracingOpenTelemetryAspect();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> aspect.around(pjp, tracingOtel));

        verify(tracingOpenTelemetry).addSpan("testMethod");
        verify(spanScope).recordException(mockThrowable);
        assertEquals("Test Exception", exception.getMessage());
    }

    @Test
    void testAddSpanIsCalledWithCorrectSignature() throws Throwable {

        when(tracingOpenTelemetry.addSpan(anyString())).thenReturn(spanScope);
        when(pjp.getSignature()).thenReturn(signature);
        Object[] args = new Object[0];
        when(pjp.getArgs()).thenReturn(args);
        when(signature.getDeclaringType()).thenReturn(RequestHandler.class);
        when(signature.getName()).thenReturn("correctMethodSignature");
        when(tracingOtel.spanName()).thenReturn("correctMethodSignature");
        when(tracingOtel.captureMode()).thenReturn(CaptureMode.ENVIRONMENT_VAR);
        when(tracingOtel.namespace()).thenReturn("test");
        when(pjp.proceed()).thenReturn("Success");

        TracingOpenTelemetryAspect aspect = new TracingOpenTelemetryAspect();
        aspect.around(pjp, tracingOtel);

        verify(tracingOpenTelemetry).addSpan("correctMethodSignature");
    }
}