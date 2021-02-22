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
import software.amazon.lambda.powertools.metrics.Metrics;
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
import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.withSingleMetric;

@Aspect
public class LambdaMetricsAspect {
    private static final String NAMESPACE = System.getenv("POWERTOOLS_METRICS_NAMESPACE");

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(metrics)")
    public void callAt(Metrics metrics) {
    }

    @Around(value = "callAt(metrics) && execution(@Metrics * *.*(..))", argNames = "pjp,metrics")
    public Object around(ProceedingJoinPoint pjp,
                         Metrics metrics) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)
                && (placedOnRequestHandler(pjp)
                || placedOnStreamHandler(pjp))) {

            MetricsLogger logger = metricsLogger();

            logger.setNamespace(namespace(metrics))
                    .putDimensions(DimensionSet.of("Service", service(metrics)));

            coldStartSingleMetricIfApplicable(pjp, metrics);

            try {
                return pjp.proceed(proceedArgs);

            } finally {
                coldStartDone();
                validateMetricsAndRefreshOnFailure(metrics);
                logger.flush();
                refreshMetricsContext();
            }
        }

        return pjp.proceed(proceedArgs);
    }

    private void coldStartSingleMetricIfApplicable(final ProceedingJoinPoint pjp,
                                                   final Metrics metrics) {
        if (metrics.captureColdStart()
                && isColdStart()) {

            Optional<Context> contextOptional = extractContext(pjp);

            if (contextOptional.isPresent()) {
                Context context = contextOptional.orElseThrow(() -> new IllegalStateException("Context not found"));

                withSingleMetric("ColdStart", 1, Unit.COUNT, namespace(metrics), (logger) ->
                        logger.setDimensions(DimensionSet.of("Service", service(metrics), "FunctionName", context.getFunctionName())));
            }
        }
    }

    private void validateBeforeFlushingMetrics(Metrics metrics) {
        if (metrics.raiseOnEmptyMetrics() && hasNoMetrics()) {
            throw new ValidationException("No metrics captured, at least one metrics must be emitted");
        }

        if (dimensionsCount() == 0 || dimensionsCount() > 9) {
            throw new ValidationException(String.format("Number of Dimensions must be in range of 1-9." +
                    " Actual size: %d.", dimensionsCount()));
        }
    }

    private String namespace(Metrics metrics) {
        return !"".equals(metrics.namespace()) ? metrics.namespace() : NAMESPACE;
    }

    private String service(Metrics metrics) {
        return !"".equals(metrics.service()) ? metrics.service() : serviceName();
    }

    private void validateMetricsAndRefreshOnFailure(Metrics metrics) {
        try {

            validateBeforeFlushingMetrics(metrics);
        } catch (ValidationException e){
            refreshMetricsContext();
            throw e;
        }
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
