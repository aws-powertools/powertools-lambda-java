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
 */
package software.amazon.lambda.powertools.kafka.internal;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;

/**
 * Utility class to determine the deserialization type from Lambda request handler methods annotated with 
 * {@link Deserialization} utility.
 * 
 * Relies on the Lambda _HANDLER environment variable to detect the currently active handler method.
 */
public final class DeserializationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeserializationUtils.class);

    private DeserializationUtils() {
    }

    public static DeserializationType determineDeserializationType() {
        String handler = System.getenv("_HANDLER");
        if (handler == null || handler.trim().isEmpty()) {
            LOGGER.error("Cannot determine deserialization type. No valid handler found in _HANDLER: {}", handler);
            return DeserializationType.LAMBDA_DEFAULT;
        }

        try {
            HandlerInfo handlerInfo = parseHandler(handler);
            Class<?> handlerClazz = Class.forName(handlerInfo.className);

            if (!RequestHandler.class.isAssignableFrom(handlerClazz)) {
                LOGGER.warn("Class '{}' does not implement RequestHandler. Ignoring.", handlerInfo.className);
                return DeserializationType.LAMBDA_DEFAULT;
            }

            return findDeserializationType(handlerClazz, handlerInfo.methodName);
        } catch (Exception e) {
            LOGGER.warn("Cannot determine deserialization type. Defaulting to standard.", e);
            return DeserializationType.LAMBDA_DEFAULT;
        }
    }

    private static HandlerInfo parseHandler(String handler) {
        if (handler.contains("::")) {
            int separatorIndex = handler.indexOf("::");
            String className = handler.substring(0, separatorIndex);
            String methodName = handler.substring(separatorIndex + 2);
            return new HandlerInfo(className, methodName);
        }

        return new HandlerInfo(handler);
    }

    private static DeserializationType findDeserializationType(Class<?> handlerClass, String methodName) {
        for (Method method : handlerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(Deserialization.class)) {
                Deserialization annotation = method.getAnnotation(Deserialization.class);
                LOGGER.debug("Found deserialization type: {}", annotation.type());
                return annotation.type();
            }
        }

        return DeserializationType.LAMBDA_DEFAULT;
    }

    private static class HandlerInfo {
        final String className;
        final String methodName;

        HandlerInfo(String className) {
            this(className, "handleRequest");
        }

        HandlerInfo(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
    }
}
