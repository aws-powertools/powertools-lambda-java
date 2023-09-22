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

package software.amazon.lambda.powertools.logging.internal;

/**
 * Json tools to serialize attributes manually, to avoid using further dependencies (jackson, gson...)
 */
public class JsonUtils {

    private JsonUtils() {
        // static utils
    }

    protected static void serializeAttribute(StringBuilder builder, String attr, String value, boolean notBegin) {
        if (value != null) {
            if (notBegin) {
                builder.append(",");
            }
            builder.append("\"").append(attr).append("\":");
            boolean isString = isString(value);
            if (isString) {
                builder.append("\"");
            }
            builder.append(value);
            if (isString) {
                builder.append("\"");
            }
        }
    }

    protected static void serializeAttribute(StringBuilder builder, String attr, String value) {
        serializeAttribute(builder, attr, value, true);
    }

    protected static void serializeAttributeAsString(StringBuilder builder, String attr, String value,
                                                     boolean notBegin) {
        if (value != null) {
            if (notBegin) {
                builder.append(",");
            }
            builder.append("\"")
                    .append(attr)
                    .append("\":\"")
                    .append(value)
                    .append("\"");
        }
    }

    protected static void serializeAttributeAsString(StringBuilder builder, String attr, String value) {
        serializeAttributeAsString(builder, attr, value, true);
    }

    /**
     * As MDC is a {@code Map<String, String>}, we need to check the type
     * to output numbers and booleans correctly (without quotes)
     */
    private static boolean isString(String str) {
        if (str == null) {
            return true;
        }
        if ("true".equals(str) || "false".equals(str)) {
            return false; // boolean
        }
        return !isNumeric(str); // number
    }

    /**
     * Taken from commons-lang3 NumberUtils to avoid include the library
     */
    private static boolean isNumeric(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        if (str.charAt(str.length() - 1) == '.') {
            return false;
        }
        if (str.charAt(0) == '-') {
            if (str.length() == 1) {
                return false;
            }
            return withDecimalsParsing(str, 1);
        }
        return withDecimalsParsing(str, 0);
    }

    /**
     * Taken from commons-lang3 NumberUtils
     */
    private static boolean withDecimalsParsing(final String str, final int beginIdx) {
        int decimalPoints = 0;
        for (int i = beginIdx; i < str.length(); i++) {
            final boolean isDecimalPoint = str.charAt(i) == '.';
            if (isDecimalPoint) {
                decimalPoints++;
            }
            if (decimalPoints > 1) {
                return false;
            }
            if (!isDecimalPoint && !Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
