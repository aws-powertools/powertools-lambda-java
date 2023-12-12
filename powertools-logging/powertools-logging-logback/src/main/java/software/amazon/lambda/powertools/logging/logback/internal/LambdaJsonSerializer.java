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

package software.amazon.lambda.powertools.logging.logback.internal;

import static java.lang.Boolean.TRUE;
import static software.amazon.lambda.powertools.logging.LoggingUtils.LOG_MESSAGES_AS_JSON;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;

/**
 * This class will serialize the log events in json.<br/>
 * <p>
 * Inspired from the ElasticSearch Serializer co.elastic.logging.EcsJsonSerializer
 */
public class LambdaJsonSerializer {
    protected static final String TIMESTAMP_ATTR_NAME = "timestamp";
    protected static final String LEVEL_ATTR_NAME = "level";
    protected static final String FORMATTED_MESSAGE_ATTR_NAME = "message";
    protected static final String THREAD_ATTR_NAME = "thread";
    protected static final String THREAD_ID_ATTR_NAME = "thread_id";
    protected static final String THREAD_PRIORITY_ATTR_NAME = "thread_priority";
    protected static final String EXCEPTION_MSG_ATTR_NAME = "message";
    protected static final String EXCEPTION_CLASS_ATTR_NAME = "name";
    protected static final String EXCEPTION_STACK_ATTR_NAME = "stack";
    protected static final String EXCEPTION_ATTR_NAME = "error";

    private LambdaJsonSerializer() {}

    public static void serializeObjectStart(StringBuilder builder) {
        builder.append('{');
    }

    public static void serializeObjectEnd(StringBuilder builder) {
        builder.append("}\n");
    }

    public static void serializeTimestamp(StringBuilder builder, long timestamp, String timestampFormat,
                                          String timestampFormatTimezoneId) {
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
        JsonUtils.serializeAttribute(builder, TIMESTAMP_ATTR_NAME, formattedTimestamp);
    }

    public static void serializeThreadName(StringBuilder builder, String threadName) {
        if (threadName != null) {
            JsonUtils.serializeAttribute(builder, THREAD_ATTR_NAME, threadName);
        }
    }

    public static void serializeLogLevel(StringBuilder builder, Level level) {
        JsonUtils.serializeAttribute(builder, LEVEL_ATTR_NAME, level.toString(), false);
    }

    public static void serializeFormattedMessage(StringBuilder builder, String message,
                                                 boolean logMessagesAsJsonGlobal, String logMessagesAsJsonLocal) {
        Boolean logMessagesAsJson = null;
        if (logMessagesAsJsonLocal != null) {
            logMessagesAsJson = Boolean.parseBoolean(logMessagesAsJsonLocal);
        }

        boolean logAsJson = ((logMessagesAsJsonGlobal && logMessagesAsJson == null) || TRUE.equals(logMessagesAsJson))
                && isValidJson(message);
        JsonUtils.serializeMessage(builder, FORMATTED_MESSAGE_ATTR_NAME, message, logAsJson);
    }

    public static void serializeException(StringBuilder builder, String className, String message, String stackTrace) {
        builder.append(",\"").append(EXCEPTION_ATTR_NAME).append("\":{");
        JsonUtils.serializeAttribute(builder, EXCEPTION_MSG_ATTR_NAME, message, false);
        JsonUtils.serializeAttribute(builder, EXCEPTION_CLASS_ATTR_NAME, className);
        JsonUtils.serializeAttribute(builder, EXCEPTION_STACK_ATTR_NAME, stackTrace);
        builder.append("}");
    }

    public static void serializeException(StringBuilder builder, Throwable throwable) {
        serializeException(builder, throwable.getClass().getName(), throwable.getMessage(),
                Arrays.toString(throwable.getStackTrace()));
    }

    public static void serializeThreadId(StringBuilder builder, String threadId) {
        JsonUtils.serializeAttribute(builder, THREAD_ID_ATTR_NAME, threadId);
    }

    public static void serializeThreadPriority(StringBuilder builder, String threadPriority) {
        JsonUtils.serializeAttribute(builder, THREAD_PRIORITY_ATTR_NAME, threadPriority);
    }

    public static void serializeEntry(StringBuilder builder, String key, Object value) {
        JsonUtils.serializeAttribute(builder, key, value.toString());
    }

    public static void serializePowertools(StringBuilder builder, Map<String, String> mdc,
                                           boolean includePowertoolsInfo) {
        TreeMap<String, String> sortedMap = new TreeMap<>(mdc);
        sortedMap.forEach((k, v) -> {
            if ((includePowertoolsInfo || !PowertoolsLoggedFields.stringValues().contains(k))  // do not log already logged powertools info
                    && !(k.equals(PowertoolsLoggedFields.SAMPLING_RATE.getName()) && v.equals("0.0"))  // do not log sampling rate when 0
                    && !LOG_MESSAGES_AS_JSON.equals(k)) // do not log internal keys
            {
                JsonUtils.serializeAttribute(builder, k, v);
            }
        });

    }

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private static boolean isValidJson(String str) {
        if (!(str.startsWith("{") || str.startsWith("["))) {
            return false;
        }
        try {
            mapper.readTree(str);
        } catch (JacksonException e) {
            return false;
        }
        return true;
    }

}
