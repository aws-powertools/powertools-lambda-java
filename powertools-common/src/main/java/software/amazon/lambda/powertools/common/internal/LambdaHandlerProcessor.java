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

package software.amazon.lambda.powertools.common.internal;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getProperty;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getenv;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import software.amazon.awssdk.utilslite.SdkInternalThreadLocal;

public final class LambdaHandlerProcessor {

    // serviceName cannot be final for testing purposes
    private static String serviceName = calculateServiceName();

    private static Boolean isColdStart = null;

    private LambdaHandlerProcessor() {
        // Hide default constructor
    }

    private static String calculateServiceName() {
        return null != getenv(LambdaConstants.POWERTOOLS_SERVICE_NAME)
                ? getenv(LambdaConstants.POWERTOOLS_SERVICE_NAME)
                : LambdaConstants.SERVICE_UNDEFINED;
    }

    public static boolean isHandlerMethod(final ProceedingJoinPoint pjp) {
        return placedOnRequestHandler(pjp) || placedOnStreamHandler(pjp);
    }

    /**
     * The class needs to implement RequestHandler interface
     * The function needs to have exactly two arguments
     * The second argument needs to be of type com.amazonaws.services.lambda.runtime.Context
     * @param pjp
     * @return
     */
    public static boolean placedOnRequestHandler(final ProceedingJoinPoint pjp) {
        return RequestHandler.class.isAssignableFrom(pjp.getSignature().getDeclaringType())
                && pjp.getArgs().length == 2
                && pjp.getArgs()[1] instanceof Context;
    }

    public static boolean placedOnStreamHandler(final ProceedingJoinPoint pjp) {
        return RequestStreamHandler.class.isAssignableFrom(pjp.getSignature().getDeclaringType())
                && pjp.getArgs().length == 3
                && pjp.getArgs()[0] instanceof InputStream
                && pjp.getArgs()[1] instanceof OutputStream
                && pjp.getArgs()[2] instanceof Context;
    }

    public static Context extractContext(final ProceedingJoinPoint pjp) {
        if (placedOnRequestHandler(pjp)) {
            return (Context) pjp.getArgs()[1];
        } else if (placedOnStreamHandler(pjp)) {
            return (Context) pjp.getArgs()[2];
        } else {
            return null;
        }
    }

    public static String serviceName() {
        return serviceName;
    }

    // Method used for testing purposes
    protected static void resetServiceName() {
        serviceName = calculateServiceName();
    }

    public static boolean isColdStart() {
        return isColdStart == null;
    }

    public static void coldStartDone() {
        isColdStart = false;
    }

    public static boolean isSamLocal() {
        return "true".equals(getenv(LambdaConstants.AWS_SAM_LOCAL));
    }

    public static Optional<String> getXrayTraceId() {
        // Try SdkInternalThreadLocal first (for concurrent Lambda environments)
        String traceId = SdkInternalThreadLocal.get(LambdaConstants.AWS_LAMBDA_X_TRACE_ID);

        // Fallback to environment variable
        if (traceId == null) {
            traceId = getenv(LambdaConstants.X_AMZN_TRACE_ID);
        }

        // For the Java Lambda 17+ runtime, the Trace ID is set as a System Property
        if (traceId == null) {
            traceId = getProperty(LambdaConstants.XRAY_TRACE_HEADER);
        }

        if (traceId != null) {
            return of(traceId.split(";")[0].replace(LambdaConstants.ROOT_EQUALS, ""));
        }
        return empty();
    }
}
