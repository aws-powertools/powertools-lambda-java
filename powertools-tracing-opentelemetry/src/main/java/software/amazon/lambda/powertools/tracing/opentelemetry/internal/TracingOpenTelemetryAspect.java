package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.tracing.opentelemetry.TracingOpenTelemetry;
import software.amazon.lambda.powertools.tracing.opentelemetry.TracingOtel;

@Aspect
public final class TracingOpenTelemetryAspect {
    //tracing cannot be final for testing purposes
    private static TracingOpenTelemetry tracing =
            TracingOpenTelemetry.create();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String COLD_START_ATTRIBUTE =
            "aws.lambda.powertools.cold_start";

    private static final String SERVICE_ATTRIBUTE =
            "aws.lambda.powertools.service";

    private static final String RESPONSE_ATTRIBUTE =
            "aws.lambda.powertools.response";

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(tracingOtel)")
    public void callAt(TracingOtel tracingOtel) {
    }

    @Around(
            value = "callAt(tracingOtel) && execution(@TracingOtel * *.*(..))",
            argNames = "pjp,tracingOtel"
    )
    public Object around(ProceedingJoinPoint pjp, TracingOtel tracingOtel) throws Throwable {

        String spanName = tracingOtel.spanName().isEmpty()
                ? pjp.getSignature().getName()
                : tracingOtel.spanName();

        String namespace = tracingOtel.namespace().isEmpty()
                ? serviceName()
                : tracingOtel.namespace();

        try (SpanScope scope = tracing.addSpan(spanName)) {

            Span span = scope.span();

            if (isHandlerMethod(pjp)) {
                span.setAttribute(COLD_START_ATTRIBUTE, isColdStart());
                span.setAttribute(SERVICE_ATTRIBUTE, namespace);
            }

            try {

                Object result = pjp.proceed(pjp.getArgs());

                if (captureResponse(tracingOtel)) {
                    span.setAttribute(RESPONSE_ATTRIBUTE, OBJECT_MAPPER.writeValueAsString(result));
                }

                if (isHandlerMethod(pjp)) {
                    coldStartDone();
                }

                return result;
            } catch (Throwable throwable) {

                if (captureError(tracingOtel)) {
                    scope.recordException(throwable);
                }
                throw throwable;
            }
        }
    }

    private boolean captureResponse(TracingOtel tracing) {
        switch (tracing.captureMode()) {
            case ENVIRONMENT_VAR:
                return isEnvironmentVariableSet("POWERTOOLS_TRACER_CAPTURE_RESPONSE")
                        && environmentVariable("POWERTOOLS_TRACER_CAPTURE_RESPONSE");
            case RESPONSE:
            case RESPONSE_AND_ERROR:
                return true;
            case DISABLED:
            case ERROR:
            default:
                return false;
        }
    }

    private boolean captureError(TracingOtel tracing) {
        switch (tracing.captureMode()) {
            case ENVIRONMENT_VAR:
                return isEnvironmentVariableSet("POWERTOOLS_TRACER_CAPTURE_ERROR")
                        && environmentVariable("POWERTOOLS_TRACER_CAPTURE_ERROR");
            case ERROR:
            case RESPONSE_AND_ERROR:
                return true;
            case DISABLED:
            case RESPONSE:
            default:
                return false;
        }
    }

    private boolean environmentVariable(String key) {
        return Boolean.parseBoolean(SystemWrapper.getenv(key));
    }

    private boolean isEnvironmentVariableSet(String key) {
        return SystemWrapper.containsKey(key);
    }

}
