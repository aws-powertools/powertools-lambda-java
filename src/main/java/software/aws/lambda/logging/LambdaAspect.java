package software.aws.lambda.logging;

import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Aspect
public final class LambdaAspect {
    static Boolean IS_COLD_START = null;

    @Pointcut("@annotation(powerToolsLogging)")
    public void callAt(PowerToolsLogging powerToolsLogging) {
    }

    @Around(value = "callAt(powerToolsLogging)")
    public Object around(ProceedingJoinPoint pjp,
                         PowerToolsLogging powerToolsLogging) throws Throwable {

        extractContext(pjp)
                .ifPresent(context -> {
                    ThreadContext.putAll(DefaultLambdaFields.values(context));
                    ThreadContext.put("coldStart", null == IS_COLD_START ? "true" : "false");
                });

        IS_COLD_START = false;


        return pjp.proceed();
    }

    private Optional<Context> extractContext(ProceedingJoinPoint pjp) {

        if ("handleRequest".equals(pjp.getSignature().getName())) {
            if (RequestHandler.class.isAssignableFrom(pjp.getSignature().getDeclaringType())
                    && pjp.getArgs().length == 2 && pjp.getArgs()[1] instanceof Context) {
                return of((Context) pjp.getArgs()[1]);
            }

            if (RequestStreamHandler.class.isAssignableFrom(pjp.getSignature().getDeclaringType())
                    && pjp.getArgs().length == 3 && pjp.getArgs()[2] instanceof Context) {
                return of((Context) pjp.getArgs()[2]);
            }
        }

        return empty();
    }
}
