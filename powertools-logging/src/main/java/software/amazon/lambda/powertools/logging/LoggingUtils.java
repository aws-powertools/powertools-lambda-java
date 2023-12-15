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

package software.amazon.lambda.powertools.logging;

import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.CORRELATION_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * A class of helper functions to add functionality to Logging.
 * Adding/removing keys is based on <a href="https://www.slf4j.org/manual.html#mdc">MDC</a>, which is ThreadSafe.
 */
public final class LoggingUtils {

    private static ObjectMapper objectMapper;

    private LoggingUtils() {
    }

    /**
     * Sets correlation id attribute on the logs.
     *
     * @param value The value of the correlation id
     */
    public static void setCorrelationId(String value) {
        MDC.put(CORRELATION_ID.getName(), value);
    }

    /**
     * Get correlation id attribute. Maybe null.
     * @return correlation id set `@Logging(correlationIdPath="JMESPATH Expression")` or `LoggingUtils.setCorrelationId("value")`
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID.getName());
    }

    /**
     * Sets the instance of ObjectMapper object which is used for serialising event when
     * {@code @Logging(logEvent = true, logResponse = true)}.
     *
     * Not Thread Safe, the object mapper is static, changing it in different threads can lead to unexpected behaviour
     *
     * @param objectMapper Custom implementation of object mapper to be used for logging serialised event
     */
    public static void setObjectMapper(ObjectMapper objectMapper) {
        LoggingUtils.objectMapper = objectMapper;
    }

    public static ObjectMapper getObjectMapper() {
        if (LoggingUtils.objectMapper == null) {
            LoggingUtils.objectMapper = JsonConfig.get().getObjectMapper();
        }
        return LoggingUtils.objectMapper;
    }
}
