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

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;

@Aspect
public class LambdaMetricsAspect {
    public static final String TRACE_ID_PROPERTY = "xray_trace_id";
    public static final String REQUEST_ID_PROPERTY = "function_request_id";
    private static final String SERVICE_DIMENSION = "Service";

    private String functionName(Metrics metrics, Context context) {
        if (!"".equals(metrics.functionName())) {
            return metrics.functionName();
        }
        return context != null ? context.getFunctionName() : null;
    }

    private String serviceNameWithFallback(Metrics metrics) {
        if (!"".equals(metrics.service())) {
            return metrics.service();
        }
        return LambdaHandlerProcessor.serviceName();
    }

    @SuppressWarnings({ "EmptyMethod" })
    @Pointcut("@annotation(metrics)")
    public void callAt(Metrics metrics) {
    }

    @Around(value = "callAt(metrics) && execution(@Metrics * *.*(..))", argNames = "pjp,metrics")
    public Object around(ProceedingJoinPoint pjp,
            Metrics metrics) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)) {
            MetricsLogger logger = MetricsLoggerFactory.getMetricsLogger();

            // The MetricsLoggerFactory applies default settings from the environment or can be configured by the
            // MetricsLoggerBuilder. We only overwrite settings if they are explicitly set in the @Metrics annotation.
            if (!"".equals(metrics.namespace())) {
                logger.setNamespace(metrics.namespace());
            }

            // If the default dimensions are larger than 1 or do not contain the "Service" dimension, it means that the
            // user overwrote them manually e.g. using MetricsLoggerBuilder. In this case, we don't set the service
            // default dimension.
            if (!"".equals(metrics.service())
                    && logger.getDefaultDimensions().getDimensionKeys().size() <= 1
                    && logger.getDefaultDimensions().getDimensionKeys().contains(SERVICE_DIMENSION)) {
                logger.setDefaultDimensions(Map.of(SERVICE_DIMENSION, metrics.service()));
            }

            logger.setRaiseOnEmptyMetrics(metrics.raiseOnEmptyMetrics());

            // Add trace ID metadata if available
            LambdaHandlerProcessor.getXrayTraceId()
                    .ifPresent(traceId -> logger.addMetadata(TRACE_ID_PROPERTY, traceId));

            Context extractedContext = extractContext(pjp);

            if (null != extractedContext) {
                logger.addMetadata(REQUEST_ID_PROPERTY, extractedContext.getAwsRequestId());

                // Only capture cold start metrics if configured
                if (metrics.captureColdStart()) {
                    // Get function name from annotation or context
                    String funcName = functionName(metrics, extractedContext);

                    // Create dimensions with service and function name
                    DimensionSet coldStartDimensions = DimensionSet.of(
                            SERVICE_DIMENSION,
                            logger.getDefaultDimensions().getDimensions().getOrDefault(SERVICE_DIMENSION,
                                    serviceNameWithFallback(metrics)),
                            "FunctionName", funcName != null ? funcName : extractedContext.getFunctionName());

                    logger.captureColdStartMetric(extractedContext, coldStartDimensions);
                }
            }

            try {
                return pjp.proceed(proceedArgs);
            } finally {
                coldStartDone();
                logger.flush();
            }
        }

        return pjp.proceed(proceedArgs);
    }
}
