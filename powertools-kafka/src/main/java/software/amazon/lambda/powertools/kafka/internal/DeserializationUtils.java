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
        try {
            // Get the handler from the environment. It has a format like org.example.MyRequestHandler::handleRequest
            String handler = System.getenv("_HANDLER");
            if (handler != null && handler.contains("::")) {
                String className = handler.substring(0, handler.indexOf("::"));
                String methodName = handler.substring(handler.indexOf("::") + 2);

                Class<?> handlerClazz = Class.forName(className);

                // Only consider if it implements RequestHandler
                if (RequestHandler.class.isAssignableFrom(handlerClazz)) {
                    // Look for deserialization type on annotation on handler method
                    for (Method method : handlerClazz.getDeclaredMethods()) {
                        if (method.getName().equals(methodName) && method.isAnnotationPresent(Deserialization.class)) {
                            Deserialization annotation = method.getAnnotation(Deserialization.class);
                            LOGGER.debug("Found deserialization type: {}", annotation.type());
                            return annotation.type();
                        }
                    }
                } else {
                    LOGGER.warn("Candidate class for custom deserialization '{}' does not implement RequestHandler. "
                            + "Ignoring.", className);
                }
            } else {
                LOGGER.error(
                        "Cannot determine deserialization type for custom deserialization. "
                                + "Defaulting to standard. "
                                + "No valid handler found in environment variable _HANDLER: {}.",
                        handler);
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Cannot determine deserialization type for custom deserialization. Defaulting to standard.",
                    e);
        }

        return DeserializationType.LAMBDA_DEFAULT;
    }
}
