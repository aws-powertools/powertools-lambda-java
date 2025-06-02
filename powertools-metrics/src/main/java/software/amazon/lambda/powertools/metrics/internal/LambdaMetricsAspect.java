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
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;

import com.amazonaws.services.lambda.runtime.Context;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;

@Aspect
public class LambdaMetricsAspect {
    public static final String TRACE_ID_PROPERTY = "xray_trace_id";
    public static final String REQUEST_ID_PROPERTY = "function_request_id";
    private static final String NAMESPACE = System.getenv("POWERTOOLS_METRICS_NAMESPACE");

    private static String service(Metrics metrics) {
        return !"".equals(metrics.service()) ? metrics.service() : serviceName();
    }
    
    private String namespace(Metrics metrics) {
        return !"".equals(metrics.namespace()) ? metrics.namespace() : NAMESPACE;
    }
    
    private String functionName(Metrics metrics, Context context) {
        if (!"".equals(metrics.functionName())) {
            return metrics.functionName();
        }
        return context != null ? context.getFunctionName() : null;
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
            MetricsLogger logger = MetricsLoggerFactory.getMetricsLogger();

            // Add service dimension separately
            logger.addDimension("Service", service(metrics));

            // Set namespace
            String metricsNamespace = namespace(metrics);
            if (metricsNamespace != null) {
                logger.setNamespace(metricsNamespace);
            }

            // Configure other settings
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
                        "Service", service(metrics),
                        "FunctionName", funcName != null ? funcName : extractedContext.getFunctionName()
                    );
                    
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