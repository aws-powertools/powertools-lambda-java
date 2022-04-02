/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.idempotency.internal;

import com.fasterxml.jackson.databind.JsonNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import software.amazon.lambda.powertools.idempotency.Constants;
import software.amazon.lambda.powertools.idempotency.IdempotencyKey;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyConfigurationException;
import software.amazon.lambda.powertools.utilities.JsonConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;

/**
 * Aspect that handles the {@link Idempotent} annotation.
 * It uses the {@link IdempotencyHandler} to actually do the job.
 */
@Aspect
public class IdempotentAspect {
    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(idempotent)")
    public void callAt(Idempotent idempotent) {
    }

    @Around(value = "callAt(idempotent) " +
      "&& execution(@software.amazon.lambda.powertools.idempotency.Idempotent * *.*(..))", argNames = "pjp,idempotent")
    public Object around(ProceedingJoinPoint pjp,
                         Idempotent idempotent) throws Throwable {

        String idempotencyDisabledEnv = System.getenv().get(Constants.IDEMPOTENCY_DISABLED_ENV);
        if (idempotencyDisabledEnv != null && !idempotencyDisabledEnv.equalsIgnoreCase("false")) {
            return pjp.proceed(pjp.getArgs());
        }

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        if (method.getReturnType().equals(void.class)) {
            throw new IdempotencyConfigurationException("The annotated method doesn't return anything. Unable to perform idempotency on void return type");
        }

        JsonNode payload = getPayload(pjp, method);
        if (payload == null) {
            throw new IdempotencyConfigurationException("Unable to get payload from the method. Ensure there is at least one parameter or that you use @IdempotencyKey");
        }

        IdempotencyHandler idempotencyHandler = new IdempotencyHandler(pjp, method.getName(), payload);
        return idempotencyHandler.handle();
    }

    /**
     * Retrieve the payload from the annotated method parameters
     * @param pjp joinPoint
     * @param method the annotated method
     * @return the payload used for idempotency
     */
    private JsonNode getPayload(ProceedingJoinPoint pjp, Method method) {
        JsonNode payload = null;
        // handleRequest or method with one parameter: get the first one
        if ((isHandlerMethod(pjp) && placedOnRequestHandler(pjp))
                || pjp.getArgs().length == 1) {
            payload = JsonConfig.get().getObjectMapper().valueToTree(pjp.getArgs()[0]);
        } else {
            // Look for a parameter annotated with @IdempotencyKey
            Annotation[][] annotations = method.getParameterAnnotations();
            for (int i = 0; i < annotations.length && payload == null; i++) {
                Annotation[] annotationsRow = annotations[i];
                for (int j = 0; j < annotationsRow.length && payload == null; j++) {
                    if (annotationsRow[j].annotationType().equals(IdempotencyKey.class)) {
                        payload = JsonConfig.get().getObjectMapper().valueToTree(pjp.getArgs()[i]);
                    }
                }
            }
        }
        return payload;
    }
}
