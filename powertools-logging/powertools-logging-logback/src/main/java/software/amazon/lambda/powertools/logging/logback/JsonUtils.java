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

package software.amazon.lambda.powertools.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.lambda.powertools.logging.argument.StructuredArgument;
import software.amazon.lambda.powertools.logging.internal.JsonSerializer;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;

/**
 * Json tools to serialize common fields
 */
final class JsonUtils {
    private static final Set<String> RESERVED_LOG_KEYS = Stream
            .concat(PowertoolsLoggedFields.stringValues().stream(),
                    List.of("message", "level", "timestamp", "error").stream())
            .collect(Collectors.toSet());

    private JsonUtils() {
        // static utils
    }

    static void serializeTimestamp(JsonSerializer generator, long timestamp, String timestampFormat,
            String timestampFormatTimezoneId, String timestampAttributeName) {
        String formattedTimestamp;
        if (timestampFormat == null || timestamp < 0) {
            formattedTimestamp = String.valueOf(timestamp);
        } else {
            Date date = new Date(timestamp);
            DateFormat format = new SimpleDateFormat(timestampFormat);

            if (timestampFormatTimezoneId != null) {
                TimeZone tz = TimeZone.getTimeZone(timestampFormatTimezoneId);
                format.setTimeZone(tz);
            }
            formattedTimestamp = format.format(date);
        }
        generator.writeStringField(timestampAttributeName, formattedTimestamp);
    }

    static void serializeMDCEntries(Map<String, String> mdcPropertyMap, JsonSerializer serializer) {
        TreeMap<String, String> sortedMap = new TreeMap<>(mdcPropertyMap);
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            if (!PowertoolsLoggedFields.stringValues().contains(entry.getKey())) {
                serializeMDCEntry(entry, serializer);
            }
        }
    }

    static void serializeMDCEntry(Map.Entry<String, String> entry, JsonSerializer serializer) {
        serializer.writeRaw(',');
        serializer.writeFieldName(entry.getKey());
        if (isString(entry.getValue())) {
            serializer.writeString(entry.getValue());
        } else {
            serializer.writeRaw(entry.getValue());
        }
    }

    static void serializeArguments(ILoggingEvent event, JsonSerializer serializer) throws IOException {
        Object[] arguments = event.getArgumentArray();
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument instanceof StructuredArgument) {
                    final StructuredArgument structArg = (StructuredArgument) argument;
                    final Iterable<String> logKeys = structArg.keys();
                    // If any of the logKeys are a reserved key we are going to ignore the argument
                    for (final String logKey : logKeys) {
                        if (RESERVED_LOG_KEYS.contains(logKey)) {
                            return;
                        }
                    }
                    serializer.writeRaw(',');
                    structArg.writeTo(serializer);
                }
            }
        }
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
