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

import static software.amazon.lambda.powertools.logging.internal.JsonUtils.serializeAttributeAsString;

import ch.qos.logback.classic.Level;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * This class will serialize the log events in ecs format (ElasticSearch).<br/>
 * <p>
 * Inspired from the ElasticSearch Serializer <code>co.elastic.logging.EcsJsonSerializer</code>, this class doesn't use
 * any JSON (de)serialization library (Jackson, Gson, etc.) to avoid the dependency
 */
public class LambdaEcsSerializer {
    protected static final String TIMESTAMP_ATTR_NAME = "@timestamp";
    protected static final String ECS_VERSION_ATTR_NAME = "ecs.version";
    protected static final String LOGGER_ATTR_NAME = "log.logger";
    protected static final String LEVEL_ATTR_NAME = "log.level";
    protected static final String SERVICE_NAME_ATTR_NAME = "service.name";
    protected static final String SERVICE_VERSION_ATTR_NAME = "service.version";
    protected static final String SERVICE_ENV_ATTR_NAME = "service.environment";
    protected static final String EVENT_DATASET_ATTR_NAME = "event.dataset";
    protected static final String FORMATTED_MESSAGE_ATTR_NAME = "message";
    protected static final String THREAD_ATTR_NAME = "process.thread.name";
    protected static final String THREAD_ID_ATTR_NAME = "process.thread.id";
    protected static final String EXCEPTION_MSG_ATTR_NAME = "error.message";
    protected static final String EXCEPTION_CLASS_ATTR_NAME = "error.type";
    protected static final String EXCEPTION_STACK_ATTR_NAME = "error.stack_trace";
    protected static final String CLOUD_PROVIDER_ATTR_NAME = "cloud.provider";
    protected static final String CLOUD_REGION_ATTR_NAME = "cloud.region";
    protected static final String CLOUD_ACCOUNT_ATTR_NAME = "cloud.account.id";
    protected static final String CLOUD_SERVICE_ATTR_NAME = "cloud.service.name";
    protected static final String FUNCTION_COLD_START_ATTR_NAME = "faas.coldstart";
    protected static final String FUNCTION_REQUEST_ID_ATTR_NAME = "faas.execution";
    protected static final String FUNCTION_ARN_ATTR_NAME = "faas.id";
    protected static final String FUNCTION_NAME_ATTR_NAME = "faas.name";
    protected static final String FUNCTION_VERSION_ATTR_NAME = "faas.version";
    protected static final String FUNCTION_MEMORY_ATTR_NAME = "faas.memory";
    protected static final String FUNCTION_TRACE_ID_ATTR_NAME = "trace.id";

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
        serializeAttributeAsString(builder, TIMESTAMP_ATTR_NAME, formattedTimestamp, false);
    }

    public static void serializeThreadName(StringBuilder builder, String threadName) {
        if (threadName != null) {
            serializeAttributeAsString(builder, THREAD_ATTR_NAME, threadName);
        }
    }

    public static void serializeLogLevel(StringBuilder builder, Level level) {
        serializeAttributeAsString(builder, LEVEL_ATTR_NAME, level.toString());
    }

    public static void serializeFormattedMessage(StringBuilder builder, String formattedMessage) {
        serializeAttributeAsString(builder, FORMATTED_MESSAGE_ATTR_NAME,
                formattedMessage.replaceAll("\"", Matcher.quoteReplacement("\\\"")));
    }

    public static void serializeException(StringBuilder builder, String className, String message, String stackTrace) {
        serializeAttributeAsString(builder, EXCEPTION_MSG_ATTR_NAME, message);
        serializeAttributeAsString(builder, EXCEPTION_CLASS_ATTR_NAME, className);
        serializeAttributeAsString(builder, EXCEPTION_STACK_ATTR_NAME, stackTrace);
    }

    public static void serializeException(StringBuilder builder, Throwable throwable) {
        serializeException(builder, throwable.getClass().getName(), throwable.getMessage(),
                Arrays.toString(throwable.getStackTrace()));
    }

    public static void serializeThreadId(StringBuilder builder, String threadId) {
        serializeAttributeAsString(builder, THREAD_ID_ATTR_NAME, threadId);
    }

    public static void serializeAdditionalFields(StringBuilder builder, Map<String, String> mdc) {
        TreeMap<String, String> sortedMap = new TreeMap<>(mdc);

        sortedMap.forEach((k, v) -> {
            if (!PowertoolsLoggedFields.stringValues().contains(k)) {
                serializeAttributeAsString(builder, k, v);
            }
        });
    }

    public static void serializeEcsVersion(StringBuilder builder, String ecsVersion) {
        serializeAttributeAsString(builder, ECS_VERSION_ATTR_NAME, ecsVersion);
    }

    public static void serializeServiceName(StringBuilder builder, String serviceName) {
        serializeAttributeAsString(builder, SERVICE_NAME_ATTR_NAME, serviceName);
    }

    public static void serializeServiceVersion(StringBuilder builder, String serviceVersion) {
        serializeAttributeAsString(builder, SERVICE_VERSION_ATTR_NAME, serviceVersion);
    }

    public static void serializeEventDataset(StringBuilder builder, String serviceName) {
        serializeAttributeAsString(builder, EVENT_DATASET_ATTR_NAME, serviceName);
    }

    public static void serializeLoggerName(StringBuilder builder, String loggerName) {
        serializeAttributeAsString(builder, LOGGER_ATTR_NAME, loggerName);
    }

    public static void serializeCloudProvider(StringBuilder builder, String cloudProvider) {
        serializeAttributeAsString(builder, CLOUD_PROVIDER_ATTR_NAME, cloudProvider);
    }

    public static void serializeCloudService(StringBuilder builder, String cloudService) {
        serializeAttributeAsString(builder, CLOUD_SERVICE_ATTR_NAME, cloudService);
    }

    public static void serializeCloudRegion(StringBuilder builder, String cloudRegion) {
        serializeAttributeAsString(builder, CLOUD_REGION_ATTR_NAME, cloudRegion);
    }

    public static void serializeCloudAccountId(StringBuilder builder, String cloudAccountId) {
        serializeAttributeAsString(builder, CLOUD_ACCOUNT_ATTR_NAME, cloudAccountId);
    }

    public static void serializeColdStart(StringBuilder builder, String coldStart) {
        serializeAttributeAsString(builder, FUNCTION_COLD_START_ATTR_NAME, coldStart);
    }

    public static void serializeFunctionExecutionId(StringBuilder builder, String requestId) {
        serializeAttributeAsString(builder, FUNCTION_REQUEST_ID_ATTR_NAME, requestId);
    }

    public static void serializeFunctionId(StringBuilder builder, String functionArn) {
        serializeAttributeAsString(builder, FUNCTION_ARN_ATTR_NAME, functionArn);
    }

    public static void serializeFunctionName(StringBuilder builder, String functionName) {
        serializeAttributeAsString(builder, FUNCTION_NAME_ATTR_NAME, functionName);
    }

    public static void serializeFunctionVersion(StringBuilder builder, String functionVersion) {
        serializeAttributeAsString(builder, FUNCTION_VERSION_ATTR_NAME, functionVersion);
    }

    public static void serializeFunctionMemory(StringBuilder builder, String functionMemory) {
        serializeAttributeAsString(builder, FUNCTION_MEMORY_ATTR_NAME, functionMemory);
    }

    public static void serializeTraceId(StringBuilder builder, String traceId) {
        serializeAttributeAsString(builder, FUNCTION_TRACE_ID_ATTR_NAME, traceId);
    }
}
