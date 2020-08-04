package software.aws.lambda.tracing.internal;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.aws.lambda.tracing.PowerToolTracing;

import static software.aws.lambda.tracing.PowerTracer.SERVICE_NAME;

@Aspect
public final class LambdaTracingAspect {
    static Boolean IS_COLD_START = null;
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

        if (placedOnHandlerMethod(pjp)) {
            segment.putAnnotation("ColdStart", IS_COLD_START == null);
        }

        IS_COLD_START = false;

        try {
            Object proceed = pjp.proceed(proceedArgs);
            if (powerToolsTracing.captureResponse()) {
                segment.putMetadata(namespace(powerToolsTracing), pjp.getSignature().getName() + " response", proceed);
            }
            return proceed;
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
        return powerToolsTracing.namespace().isEmpty() ? SERVICE_NAME : powerToolsTracing.namespace();
    }

    // TODO enrich to check more like inherited class
    private boolean placedOnHandlerMethod(ProceedingJoinPoint pjp) {
        return "handleRequest".equals(pjp.getSignature().getName());
    }
}
