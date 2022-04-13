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
package software.amazon.lambda.powertools.cors.internal;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import software.amazon.lambda.powertools.cors.CrossOrigin;

import java.lang.reflect.Method;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;

@Aspect
public class CrossOriginAspect {

    private static final Logger LOG = LogManager.getLogger(CrossOriginAspect.class);

    @Pointcut("@annotation(crossOrigin)")
    public void callAt(CrossOrigin crossOrigin) {
    }

    @Around(value = "callAt(crossOrigin) && execution(@CrossOrigin * *.*(..))", argNames = "pjp,crossOrigin")
    public Object around(ProceedingJoinPoint pjp,
                         CrossOrigin crossOrigin) throws Throwable {
        Object result = pjp.proceed(pjp.getArgs());

        if (!isHandlerMethod(pjp) || !placedOnRequestHandler(pjp) || !isApiGatewayRequest(pjp)) {
            LOG.warn("@Cors annotation must be used on a Lambda handler that receives APIGatewayProxyRequestEvent and return APIGatewayProxyResponseEvent");
            return result;
        }

        CrossOriginHandler crossOriginHandler = new CrossOriginHandler(crossOrigin);
        return proceed(pjp, result, crossOriginHandler);
    }

    private Object proceed(ProceedingJoinPoint pjp, Object result, CrossOriginHandler crossOriginHandler) {
        try {
            APIGatewayProxyRequestEvent request = (APIGatewayProxyRequestEvent) pjp.getArgs()[0];
            APIGatewayProxyResponseEvent response = (APIGatewayProxyResponseEvent) result;

            return crossOriginHandler.process(request, response);
        } catch (Exception e) {
            // should not happen, but we don't want to fail because of this
            LOG.error("Error while setting CORS headers. If you think this is an issue in PowerTools, please open an issue on GitHub.", e);
        }
        return result;
    }

    private boolean isApiGatewayRequest(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        return method.getReturnType().equals(APIGatewayProxyResponseEvent.class) &&
                pjp.getArgs()[0] instanceof APIGatewayProxyRequestEvent;
    }
}
