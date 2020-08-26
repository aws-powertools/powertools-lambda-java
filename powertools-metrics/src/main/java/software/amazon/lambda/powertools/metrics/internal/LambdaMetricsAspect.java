package software.amazon.lambda.powertools.metrics.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

@Aspect
public class LambdaMetricsAspect {

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(powertoolsMetrics)")
    public void callAt(PowertoolsMetrics powertoolsMetrics) {
    }

    @Around(value = "callAt(powertoolsMetrics) && execution(@PowertoolsMetrics * *.*(..))", argNames = "pjp,powertoolsMetrics")
    public Object around(ProceedingJoinPoint pjp,
                         PowertoolsMetrics powertoolsMetrics) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        System.out.println("hello aspect");


        PowertoolsMetric.get
        metricsLogger.flush();
        return pjp.proceed(proceedArgs);
    }
}
