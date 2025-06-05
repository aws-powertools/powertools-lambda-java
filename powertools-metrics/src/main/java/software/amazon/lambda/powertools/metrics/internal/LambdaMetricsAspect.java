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

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.common.internal.LambdaConstants;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;

@Aspect
public class LambdaMetricsAspect {
    public static final String TRACE_ID_PROPERTY = "xray_trace_id";
    public static final String REQUEST_ID_PROPERTY = "function_request_id";
    private static final String SERVICE_DIMENSION = "Service";
    private static final String FUNCTION_NAME_ENV_VAR = "POWERTOOLS_METRICS_FUNCTION_NAME";

    private String functionName(FlushMetrics metrics, Context context) {
        if (!"".equals(metrics.functionName())) {
            return metrics.functionName();
        }

        String envFunctionName = System.getenv(FUNCTION_NAME_ENV_VAR);
        if (envFunctionName != null && !envFunctionName.isEmpty()) {
            return envFunctionName;
        }

        return context != null ? context.getFunctionName() : null;
    }

    private String serviceNameWithFallback(FlushMetrics metrics) {
        if (!"".equals(metrics.service())) {
            return metrics.service();
        }
        return LambdaHandlerProcessor.serviceName();
    }

    @SuppressWarnings({ "EmptyMethod" })
    @Pointcut("@annotation(metrics)")
    public void callAt(FlushMetrics metrics) {
    }

    @Around(value = "callAt(metrics) && execution(@FlushMetrics * *.*(..))", argNames = "pjp,metrics")
    public Object around(ProceedingJoinPoint pjp,
            FlushMetrics metrics) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)) {
            Metrics metricsInstance = MetricsFactory.getMetricsInstance();

            // The MetricsFactory applies default settings from the environment or can be configured by the
            // MetricsBuilder. We only overwrite settings if they are explicitly set in the @FlushMetrics
            // annotation.
            if (!"".equals(metrics.namespace())) {
                metricsInstance.setNamespace(metrics.namespace());
            }

            // We only overwrite the default dimensions if the user didn't overwrite them previously. This means that
            // they are either empty or only contain the default "Service" dimension.
            if (!"".equals(metrics.service().trim()) && (metricsInstance.getDefaultDimensions().getDimensionKeys().size() <= 1
                    || metricsInstance.getDefaultDimensions().getDimensionKeys().contains(SERVICE_DIMENSION))) {
                metricsInstance.setDefaultDimensions(DimensionSet.of(SERVICE_DIMENSION, metrics.service()));
            }

            metricsInstance.setRaiseOnEmptyMetrics(metrics.raiseOnEmptyMetrics());

            // Add trace ID metadata if available
            LambdaHandlerProcessor.getXrayTraceId()
                    .ifPresent(traceId -> metricsInstance.addMetadata(TRACE_ID_PROPERTY, traceId));

            Context extractedContext = extractContext(pjp);

            if (null != extractedContext) {
                metricsInstance.addMetadata(REQUEST_ID_PROPERTY, extractedContext.getAwsRequestId());

                // Only capture cold start metrics if configured
                if (metrics.captureColdStart()) {
                    // Get function name from annotation or context
                    String funcName = functionName(metrics, extractedContext);

                    DimensionSet coldStartDimensions = new DimensionSet();

                    // Get service name from metrics instance default dimensions or fallback
                    String serviceName = metricsInstance.getDefaultDimensions().getDimensions().getOrDefault(SERVICE_DIMENSION,
                            serviceNameWithFallback(metrics));

                    // Only add service if it is not undefined
                    if (!LambdaConstants.SERVICE_UNDEFINED.equals(serviceName)) {
                        coldStartDimensions.addDimension(SERVICE_DIMENSION, serviceName);
                    }

                    // Add function name
                    coldStartDimensions.addDimension("FunctionName",
                            funcName != null ? funcName : extractedContext.getFunctionName());

                    metricsInstance.captureColdStartMetric(extractedContext, coldStartDimensions);
                }
            }

            try {
                return pjp.proceed(proceedArgs);
            } finally {
                coldStartDone();
                metricsInstance.flush();
            }
        }

        return pjp.proceed(proceedArgs);
    }
}
