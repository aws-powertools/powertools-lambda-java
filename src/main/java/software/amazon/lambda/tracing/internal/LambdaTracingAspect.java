package software.amazon.lambda.tracing.internal;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.tracing.PowerToolTracing;
import software.amazon.lambda.tracing.PowerTracer;

import static software.amazon.lambda.internal.LambdaHandlerProcessor.IS_COLD_START;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.placedOnStreamHandler;

@Aspect
public final class LambdaTracingAspect {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Pointcut("@annotation(powerToolsTracing)")
    public void callAt(PowerToolTracing powerToolsTracing) {
    }

    @Around(value = "callAt(powerToolsTracing) && execution(@PowerToolTracing * *.*(..))", argNames = "pjp,powerToolsTracing")
    public Object around(ProceedingJoinPoint pjp,
                         PowerToolTracing powerToolsTracing) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        Subsegment segment;

        segment = AWSXRay.beginSubsegment("## " + pjp.getSignature().getName());
        segment.setNamespace(namespace(powerToolsTracing));

        boolean placedOnHandlerMethod = placedOnHandlerMethod(pjp);

        if (placedOnHandlerMethod) {
            segment.putAnnotation("ColdStart", IS_COLD_START == null);
        }


        try {
            Object methodReturn = pjp.proceed(proceedArgs);
            if (powerToolsTracing.captureResponse()) {
                segment.putMetadata(namespace(powerToolsTracing), pjp.getSignature().getName() + " response", methodReturn);
            }

            IS_COLD_START = false;

            return methodReturn;
        } catch (Exception e) {
            if (powerToolsTracing.captureError()) {
                segment.putMetadata(namespace(powerToolsTracing), pjp.getSignature().getName() + " error", e);
            }
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    private String namespace(PowerToolTracing powerToolsTracing) {
        return powerToolsTracing.namespace().isEmpty() ? PowerTracer.SERVICE_NAME : powerToolsTracing.namespace();
    }

    private boolean placedOnHandlerMethod(ProceedingJoinPoint pjp) {
        return isHandlerMethod(pjp)
                && (placedOnRequestHandler(pjp) || placedOnStreamHandler(pjp));
    }
}
