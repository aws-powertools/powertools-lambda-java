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
package software.amazon.lambda.powertools.logging;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;

/**
 * A class of helper functions to add additional functionality to Logging.
 *
 * {@see Logging}
 */
public final class LoggingUtils {
    private static ObjectMapper objectMapper = new ObjectMapper();

    private LoggingUtils() {
    }

    /**
     * Appends an additional key and value to each log entry made. Duplicate values
     * for the same key will be replaced with the latest.
     *
     * @param key The name of the key to be logged
     * @param value The value to be logged
     */
    public static void appendKey(String key, String value) {
        ThreadContext.put(key, value);
    }


    /**
     * Appends additional key and value to each log entry made. Duplicate values
     * for the same key will be replaced with the latest.
     *
     * @param customKeys Map of custom keys values to be appended to logs
     */
    public static void appendKeys(Map<String, String> customKeys) {
        ThreadContext.putAll(customKeys);
    }

    /**
     * Sets the instance of ObjectMapper object which is used for serialising event when
     * {@code @Logging(logEvent = true)}.
     *
     * @param objectMapper Custom implementation of object mapper to be used for logging serialised event
     */
    public static void defaultObjectMapper(ObjectMapper objectMapper) {
        LoggingUtils.objectMapper = objectMapper;
    }

    public static ObjectMapper objectMapper() {
        return objectMapper;
    }
}
