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
import java.util.Arrays;
import java.util.Map;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * A class of helper functions to add additional functionality to Logging.
 * <p>
 * {@see Logging}
 */
public final class LoggingUtils {

    public static final String LOG_MESSAGES_AS_JSON = "PowertoolsLogMessagesAsJson";

    private static ObjectMapper objectMapper;

    private LoggingUtils() {
    }

    /**
     * Appends an additional key and value to each log entry made. Duplicate values
     * for the same key will be replaced with the latest.
     *
     * @param key   The name of the key to be logged
     * @param value The value to be logged
     */
    public static void appendKey(String key, String value) {
        MDC.put(key, value);
    }


    /**
     * Appends additional key and value to each log entry made. Duplicate values
     * for the same key will be replaced with the latest.
     *
     * @param customKeys Map of custom keys values to be appended to logs
     */
    public static void appendKeys(Map<String, String> customKeys) {
        customKeys.forEach(MDC::put);
    }

    /**
     * Remove an additional key from log entry.
     *
     * @param customKey The name of the key to be logged
     */
    public static void removeKey(String customKey) {
        MDC.remove(customKey);
    }


    /**
     * Removes additional keys from log entry.
     *
     * @param keys Map of custom keys values to be appended to logs
     */
    public static void removeKeys(String... keys) {
        Arrays.stream(keys).forEach(MDC::remove);
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
     * When set to true, will log messages as JSON (without escaping string).
     * Useful to log events or big JSON objects.
     * @param value boolean to specify if yes or no messages should be logged as JSON (default is false)
     */
    public static void logMessagesAsJson(boolean value) {
        MDC.put(LOG_MESSAGES_AS_JSON, String.valueOf(value));
    }

    /**
     * Sets the instance of ObjectMapper object which is used for serialising event when
     * {@code @Logging(logEvent = true)}.
     *
     * @param objectMapper Custom implementation of object mapper to be used for logging serialised event
     */
    public static void setObjectMapper(ObjectMapper objectMapper) {
        LoggingUtils.objectMapper = objectMapper;
    }

    public static ObjectMapper getObjectMapper() {
        if (null == LoggingUtils.objectMapper) {
            LoggingUtils.objectMapper = JsonConfig.get().getObjectMapper();
        }
        return LoggingUtils.objectMapper;
    }
}
