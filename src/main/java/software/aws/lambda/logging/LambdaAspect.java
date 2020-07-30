package software.aws.lambda.logging;

import com.amazonaws.services.lambda.runtime.Context;
import org.apache.logging.log4j.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public final class LambdaAspect {
    private static Boolean IS_COLD_START = null;

    @Pointcut("@annotation(powerToolsLogging)")
    public void callAt(PowerToolsLogging powerToolsLogging) {
    }

    @Around(value = "callAt(powerToolsLogging)")
    public Object around(ProceedingJoinPoint pjp,
                         PowerToolsLogging powerToolsLogging) throws Throwable {

        // TODO JoinPoint that annotation is used on entry method of lambda or do we want it to work anywhere
        if(powerToolsLogging.injectContextInfo()) {
            if(pjp.getArgs().length == 2 && pjp.getArgs()[1] instanceof Context)  {
                ThreadContext.putAll(DefaultLambdaFields.values((Context) pjp.getArgs()[1]));
            }
        }

        ThreadContext.put("coldStart", null == IS_COLD_START? "true" : "false");

        IS_COLD_START = false;

        return pjp.proceed();
    }
}
