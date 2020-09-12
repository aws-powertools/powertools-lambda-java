/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.tracing.internal;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.tracing.PowertoolsTracing;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;

@Aspect
public final class LambdaTracingAspect {

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(powerToolsTracing)")
    public void callAt(PowertoolsTracing powerToolsTracing) {
    }

    @Around(value = "callAt(powerToolsTracing) && execution(@PowertoolsTracing * *.*(..))", argNames = "pjp,powerToolsTracing")
    public Object around(ProceedingJoinPoint pjp,
                         PowertoolsTracing powerToolsTracing) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        Subsegment segment = AWSXRay.beginSubsegment("## " + pjp.getSignature().getName());
        segment.setNamespace(namespace(powerToolsTracing));

        if (placedOnHandlerMethod(pjp)) {
            segment.putAnnotation("ColdStart", isColdStart());
        }

        try {
            Object methodReturn = pjp.proceed(proceedArgs);
            if (powerToolsTracing.captureResponse()) {
                segment.putMetadata(namespace(powerToolsTracing), pjp.getSignature().getName() + " response", methodReturn);
            }

            coldStartDone();
            return methodReturn;
        } catch (Exception e) {
            if (powerToolsTracing.captureError()) {
                segment.putMetadata(namespace(powerToolsTracing), pjp.getSignature().getName() + " error", e);
            }
            throw e;
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    private String namespace(PowertoolsTracing powerToolsTracing) {
        return powerToolsTracing.namespace().isEmpty() ? serviceName() : powerToolsTracing.namespace();
    }

    private boolean placedOnHandlerMethod(ProceedingJoinPoint pjp) {
        return isHandlerMethod(pjp)
                && (placedOnRequestHandler(pjp) || placedOnStreamHandler(pjp));
    }
}
