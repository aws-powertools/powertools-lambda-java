/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.metrics.internal;

import static software.amazon.cloudwatchlogs.emf.model.MetricsLoggerHelper.dimensionsCount;
import static software.amazon.cloudwatchlogs.emf.model.MetricsLoggerHelper.hasNoMetrics;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.hasDefaultDimension;
import static software.amazon.lambda.powertools.metrics.MetricsUtils.metricsLogger;

import com.amazonaws.services.lambda.runtime.Context;
import java.lang.reflect.Field;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.crac.Core;
import org.crac.Resource;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.common.internal.ClassPreLoader;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.metrics.ValidationException;

@Aspect
public class LambdaMetricsAspect implements Resource {
    public static final String TRACE_ID_PROPERTY = "xray_trace_id";
    public static final String REQUEST_ID_PROPERTY = "function_request_id";
    private static final String NAMESPACE = System.getenv("POWERTOOLS_METRICS_NAMESPACE");

    public LambdaMetricsAspect() {
        Core.getGlobalContext().register(this);
    }

    private static String service(Metrics metrics) {
        return !"".equals(metrics.service()) ? metrics.service() : serviceName();
    }

    // This can be simplified after this issues https://github.com/awslabs/aws-embedded-metrics-java/issues/35 is fixed
    public static void refreshMetricsContext(Metrics metrics) {
        try {
            Field f = metricsLogger().getClass().getDeclaredField("context");
            f.setAccessible(true);
            MetricsContext context = new MetricsContext();

            DimensionSet[] defaultDimensions = hasDefaultDimension() ? MetricsUtils.getDefaultDimensions()
                    : new DimensionSet[] {DimensionSet.of("Service", service(metrics))};

            context.setDimensions(defaultDimensions);

            f.set(metricsLogger(), context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(metrics)")
    public void callAt(Metrics metrics) {
    }

    @Around(value = "callAt(metrics) && execution(@Metrics * *.*(..))", argNames = "pjp,metrics")
    public Object around(ProceedingJoinPoint pjp,
                         Metrics metrics) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)) {

            MetricsLogger logger = metricsLogger();

            refreshMetricsContext(metrics);

            logger.setNamespace(namespace(metrics));

            Context extractedContext = extractContext(pjp);

            if (null != extractedContext) {
                coldStartSingleMetricIfApplicable(extractedContext.getAwsRequestId(),
                        extractedContext.getFunctionName(), metrics);
                logger.putProperty(REQUEST_ID_PROPERTY, extractedContext.getAwsRequestId());
            }

            LambdaHandlerProcessor.getXrayTraceId()
                    .ifPresent(traceId -> logger.putProperty(TRACE_ID_PROPERTY, traceId));

            try {
                return pjp.proceed(proceedArgs);

            } finally {
                coldStartDone();
                validateMetricsAndRefreshOnFailure(metrics);
                logger.flush();
                refreshMetricsContext(metrics);
            }
        }

        return pjp.proceed(proceedArgs);
    }

    private void coldStartSingleMetricIfApplicable(final String awsRequestId,
                                                   final String functionName,
                                                   final Metrics metrics) {
        if (metrics.captureColdStart()
                && isColdStart()) {
            MetricsLogger metricsLogger = new MetricsLogger();
            metricsLogger.setNamespace(namespace(metrics));
            metricsLogger.putMetric("ColdStart", 1, Unit.COUNT);
            metricsLogger.setDimensions(DimensionSet.of("Service", service(metrics), "FunctionName", functionName));
            metricsLogger.putProperty(REQUEST_ID_PROPERTY, awsRequestId);
            metricsLogger.flush();
        }

    }

    private void validateBeforeFlushingMetrics(Metrics metrics) {
        if (metrics.raiseOnEmptyMetrics() && hasNoMetrics()) {
            throw new ValidationException("No metrics captured, at least one metrics must be emitted");
        }

        if (dimensionsCount() > 9) {
            throw new ValidationException(String.format("Number of Dimensions must be in range of 0-9." +
                    " Actual size: %d.", dimensionsCount()));
        }
    }

    private String namespace(Metrics metrics) {
        return !"".equals(metrics.namespace()) ? metrics.namespace() : NAMESPACE;
    }

    private void validateMetricsAndRefreshOnFailure(Metrics metrics) {
        try {
            validateBeforeFlushingMetrics(metrics);
        } catch (ValidationException e) {
            refreshMetricsContext(metrics);
            throw e;
        }
    }

    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context) throws Exception {
        System.out.println("before preloading");
        ClassPreLoader.preloadClasses(true);
        System.out.println("after preloading");
    }
    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {
    }
}
