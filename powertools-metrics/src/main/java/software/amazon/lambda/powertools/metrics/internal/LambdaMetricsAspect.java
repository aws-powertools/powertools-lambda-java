package software.amazon.lambda.powertools.metrics.internal;

import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger.metricsLogger;

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

            MetricsLogger logger = metricsLogger();

            logger.setNamespace(namespace(powertoolsMetrics))
                    .setDimensions(DimensionSet.of("service", service(powertoolsMetrics)));

            MetricsLogger coldStartLogger = null;

            if (powertoolsMetrics.captureColdStart()
                    && isColdStart()) {

                Optional<Context> contextOptional = extractContext(pjp);

                if (contextOptional.isPresent()) {
                    Context context = contextOptional.orElseThrow(() -> new IllegalStateException("Context not found"));

                    coldStartLogger = new MetricsLogger();

                    coldStartLogger.putDimensions(DimensionSet.of("function_name", context.getFunctionName()))
                            .setDimensions(DimensionSet.of("service", service(powertoolsMetrics)))
                            .setNamespace(namespace(powertoolsMetrics))
                            .putMetric("ColdStart", 1, Unit.COUNT);
                }
            }

            Object proceed = pjp.proceed(proceedArgs);

            if (null != coldStartLogger) {
                coldStartLogger.flush();
            }

            coldStartDone();
            logger.flush();
            return proceed;
        }

        return pjp.proceed(proceedArgs);
    }

    private String namespace(PowertoolsMetrics powertoolsMetrics) {
        return !"".equals(powertoolsMetrics.namespace()) ? powertoolsMetrics.namespace() : NAMESPACE;
    }

    private String service(PowertoolsMetrics powertoolsMetrics) {
        return !"".equals(powertoolsMetrics.service()) ? powertoolsMetrics.service() : serviceName();
    }
}
