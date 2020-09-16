package software.amazon.lambda.powertools.metrics.internal;

import java.lang.reflect.Field;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;
import software.amazon.lambda.powertools.metrics.ValidationException;

import static software.amazon.cloudwatchlogs.emf.model.MetricsLoggerHelper.dimensionsCount;
import static software.amazon.cloudwatchlogs.emf.model.MetricsLoggerHelper.hasNoMetrics;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger.metricsLogger;
import static software.amazon.lambda.powertools.metrics.PowertoolsMetricsLogger.withSingleMetric;

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
                    .putDimensions(DimensionSet.of("Service", service(powertoolsMetrics)));

            coldStartSingleMetricIfApplicable(pjp, powertoolsMetrics);

            try {
                Object proceed = pjp.proceed(proceedArgs);

                coldStartDone();

                validateBeforeFlushingMetrics(powertoolsMetrics);

                logger.flush();
                return proceed;

            } finally {
                refreshMetricsContext();
            }
        }

        return pjp.proceed(proceedArgs);
    }

    private void coldStartSingleMetricIfApplicable(final ProceedingJoinPoint pjp,
                                                   final PowertoolsMetrics powertoolsMetrics) {
        if (powertoolsMetrics.captureColdStart()
                && isColdStart()) {

            Optional<Context> contextOptional = extractContext(pjp);

            if (contextOptional.isPresent()) {
                Context context = contextOptional.orElseThrow(() -> new IllegalStateException("Context not found"));

                withSingleMetric("ColdStart", 1, Unit.COUNT, namespace(powertoolsMetrics), (logger) ->
                        logger.setDimensions(DimensionSet.of("Service", service(powertoolsMetrics), "FunctionName", context.getFunctionName())));
            }
        }
    }

    private void validateBeforeFlushingMetrics(PowertoolsMetrics powertoolsMetrics) {
        if (powertoolsMetrics.raiseOnEmptyMetrics() && hasNoMetrics()) {
            throw new ValidationException("No metrics captured, at least one metrics must be emitted");
        }

        if (dimensionsCount() == 0 || dimensionsCount() > 9) {
            throw new ValidationException(String.format("Number of Dimensions must be in range of 1-9." +
                    " Actual size: %d.", dimensionsCount()));
        }
    }

    private String namespace(PowertoolsMetrics powertoolsMetrics) {
        return !"".equals(powertoolsMetrics.namespace()) ? powertoolsMetrics.namespace() : NAMESPACE;
    }

    private String service(PowertoolsMetrics powertoolsMetrics) {
        return !"".equals(powertoolsMetrics.service()) ? powertoolsMetrics.service() : serviceName();
    }

    // This can be simplified after this issues https://github.com/awslabs/aws-embedded-metrics-java/issues/35 is fixed
    private static void refreshMetricsContext() {
        try {
            Field f = metricsLogger().getClass().getDeclaredField("context");
            f.setAccessible(true);
            f.set(metricsLogger(), new MetricsContext());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
