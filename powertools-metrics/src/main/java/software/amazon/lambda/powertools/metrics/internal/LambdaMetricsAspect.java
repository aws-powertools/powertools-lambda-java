package software.amazon.lambda.powertools.metrics.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger.logger;

@Aspect
public class LambdaMetricsAspect {
    private static final String NAMESPACE = System.getenv("POWERTOOLS_METRICS_NAMESPACE");

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(powertoolsMetrics)")
    public void callAt(PowertoolsMetrics powertoolsMetrics) {
    }

    @Around(value = "callAt(powertoolsMetrics) && execution(@PowertoolsMetrics * *.*(..))", argNames = "pjp,powertoolsMetrics")
    public Object around(ProceedingJoinPoint pjp,
                         PowertoolsMetrics powertoolsMetrics) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)
                && (placedOnRequestHandler(pjp)
                || placedOnStreamHandler(pjp))) {

            MetricsLogger logger = logger();

            logger.setNamespace(!"".equals(powertoolsMetrics.namespace()) ? powertoolsMetrics.namespace() : NAMESPACE);
            logger.setDimensions(DimensionSet.of("service", !"".equals(powertoolsMetrics.service()) ? powertoolsMetrics.service() : serviceName()));

            Object proceed = pjp.proceed(proceedArgs);

            logger.flush();

            return proceed;
        }

        return pjp.proceed(proceedArgs);
    }
}
