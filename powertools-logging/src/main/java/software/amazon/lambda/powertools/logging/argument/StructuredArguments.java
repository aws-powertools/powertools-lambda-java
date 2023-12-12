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

package software.amazon.lambda.powertools.logging.argument;

import java.util.Arrays;
import java.util.Map;

/**
 * Factory for creating {@link StructuredArgument}s.
 *
 * Inspired from the StructuredArgument of logstash-logback-encoder.
 */
public class StructuredArguments {

    private StructuredArguments() {
        // nothing to do, use static methods only
    }

    /**
     * Adds "key": "value" to the JSON structure and "key=value" to the formatted message.
     *
     * @param key the field name
     * @param value the value associated with the key (can be any kind of object)
     * @return a {@link StructuredArgument} populated with the data
     */
    public static StructuredArgument entry(String key, Object value) {
        return new KeyValueArgument(key, value);
    }

    /**
     * Adds a "key": "value" to the JSON structure for each entry in the map
     * and {@code map.toString()} to the formatted message.
     *
     * @param map {@link Map} holding the key/value pairs
     * @return a {@link MapArgument} populated with the data
     */
    public static StructuredArgument entries(Map<?, ?> map) {
        return new MapArgument(map);
    }

    /**
     * Adds a field to the JSON structure with key as the key and where value
     * is a JSON array of objects AND a string version of the array to the formatted message:
     * {@code "key": [value, value]}
     *
     * @param key the field name
     * @param values elements of the array associated with the key
     * @return an {@link ArrayArgument} populated with the data
     */
    public static StructuredArgument array(String key, Object... values) {
        return new ArrayArgument(key, values);
    }

    /**
     * Adds the {@code rawJson} to the JSON structure and
     * the {@code rawJson} to the formatted message.
     *
     * @param key the field name
     * @param rawJson the raw JSON String
     * @return a {@link JsonArgument} populated with the data
     */
    public static StructuredArgument json(String key, String rawJson) {
        return new JsonArgument(key, rawJson);
    }

    /**
     * Format the argument into a string.
     *
     * This method mimics the slf4j behavior:
     * array objects are formatted as array using {@link Arrays#toString},
     * non array object using {@link String#valueOf}.
     *
     * <p>See org.slf4j.helpers.MessageFormatter#deeplyAppendParameter(StringBuilder, Object, Map)}
     *
     * @param arg the argument to format
     * @return formatted string version of the argument
     */
    public static String toString(Object arg) {

        if (arg == null) {
            return "null";
        }

        Class<?> argClass = arg.getClass();

        try {
            if (!argClass.isArray()) {
                return String.valueOf(arg);
            } else {
                if (argClass == byte[].class) {
                    return Arrays.toString((byte[]) arg);
                } else if (argClass == short[].class) {
                    return Arrays.toString((short[]) arg);
                } else if (argClass == int[].class) {
                    return Arrays.toString((int[]) arg);
                } else if (argClass == long[].class) {
                    return Arrays.toString((long[]) arg);
                } else if (argClass == char[].class) {
                    return Arrays.toString((char[]) arg);
                } else if (argClass == float[].class) {
                    return Arrays.toString((float[]) arg);
                } else if (argClass == double[].class) {
                    return Arrays.toString((double[]) arg);
                } else if (argClass == boolean[].class) {
                    return Arrays.toString((boolean[]) arg);
                } else {
                    return Arrays.deepToString((Object[]) arg);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed toString() invocation on an object of type [" + argClass.getName() + "]");
            return "[FAILED toString()]";
        }
    }

}
