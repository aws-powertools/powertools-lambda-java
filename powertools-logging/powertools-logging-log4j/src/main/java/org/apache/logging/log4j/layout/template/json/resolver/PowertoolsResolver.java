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

package org.apache.logging.log4j.layout.template.json.resolver;

import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_ARN;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_NAME;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_REQUEST_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_VERSION;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SAMPLING_RATE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SERVICE;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import software.amazon.lambda.powertools.common.internal.LambdaConstants;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;

/**
 * Custom {@link org.apache.logging.log4j.layout.template.json.resolver.TemplateResolver}
 * used by {@link org.apache.logging.log4j.layout.template.json.JsonTemplateLayout}
 * to be able to recognize powertools fields in the LambdaJsonLayout.json file.
 */
final class PowertoolsResolver implements EventResolver {

    private static final EventResolver COLD_START_RESOLVER = new EventResolver() {
        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final String coldStart =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_COLD_START.getName());
            return null != coldStart;
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String coldStart =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_COLD_START.getName());
            jsonWriter.writeBoolean(Boolean.parseBoolean(coldStart));
        }
    };

    private static final EventResolver FUNCTION_NAME_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String functionName =
                        logEvent.getContextData().getValue(FUNCTION_NAME.getName());
                jsonWriter.writeString(functionName);
            };

    private static final EventResolver FUNCTION_VERSION_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String functionVersion =
                        logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_VERSION.getName());
                jsonWriter.writeString(functionVersion);
            };

    private static final EventResolver FUNCTION_ARN_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String functionArn =
                        logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_ARN.getName());
                jsonWriter.writeString(functionArn);
            };

    private static final EventResolver FUNCTION_REQ_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String functionRequestId =
                        logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_REQUEST_ID.getName());
                jsonWriter.writeString(functionRequestId);
            };

    private static final EventResolver FUNCTION_MEMORY_RESOLVER = new EventResolver() {
        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final String functionMemory =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE.getName());
            return null != functionMemory;
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String functionMemory =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE.getName());
            jsonWriter.writeNumber(Integer.parseInt(functionMemory));
        }
    };

    private static final EventResolver SAMPLING_RATE_RESOLVER = new EventResolver() {
        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final String samplingRate =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.SAMPLING_RATE.getName());
            try {
                return (null != samplingRate && Float.parseFloat(samplingRate) > 0.f);
            } catch (NumberFormatException nfe) {
                return false;
            }
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String samplingRate =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.SAMPLING_RATE.getName());
            jsonWriter.writeNumber(Float.parseFloat(samplingRate));
        }
    };

    private static final EventResolver XRAY_TRACE_RESOLVER = new EventResolver() {
        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final String traceId =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_TRACE_ID.getName());
            return null != traceId;
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String traceId =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_TRACE_ID.getName());
            jsonWriter.writeString(traceId);
        }
    };

    private static final EventResolver SERVICE_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String service = logEvent.getContextData().getValue(PowertoolsLoggedFields.SERVICE.getName());
                jsonWriter.writeString(service);
            };

    private static final EventResolver REGION_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) ->
                    jsonWriter.writeString(SystemWrapper.getenv(LambdaConstants.AWS_REGION_ENV));

    public static final String LAMBDA_ARN_REGEX =
            "^arn:(aws|aws-us-gov|aws-cn):lambda:[a-zA-Z0-9-]+:\\d{12}:function:[a-zA-Z0-9-_]+(:[a-zA-Z0-9-_]+)?$";

    private static final EventResolver ACCOUNT_ID_RESOLVER = new EventResolver() {
        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final String arn = logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_ARN.getName());
            return null != arn && !arn.isEmpty() && arn.matches(LAMBDA_ARN_REGEX);
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String arn = logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_ARN.getName());
            jsonWriter.writeString(arn.split(":")[4]);
        }
    };

    /**
     * Use a custom message resolver to permit to log json string in json format without escaped quotes.
     */
    private static final EventResolver MESSAGE_RESOLVER = new EventResolver() {
        private final ObjectMapper mapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

        public boolean isValidJson(String json) {
            if (!(json.startsWith("{") || json.startsWith("["))) {
                return false;
            }
            try {
                mapper.readTree(json);
            } catch (JacksonException e) {
                return false;
            }
            return true;
        }

        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final Message msg = logEvent.getMessage();
            return null != msg && null != msg.getFormattedMessage();
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            String message = logEvent.getMessage().getFormattedMessage();
            if (isValidJson(message)) {
                jsonWriter.writeRawString(message);
            } else {
                jsonWriter.writeString(message);
            }
        }
    };

    private static final EventResolver NON_POWERTOOLS_FIELD_RESOLVER =
            (LogEvent logEvent, JsonWriter jsonWriter) -> {
                StringBuilder stringBuilder = jsonWriter.getStringBuilder();
                // remove dummy field to kick in powertools resolver
                stringBuilder.setLength(stringBuilder.length() - 4);

                // Inject all the context information.
                ReadOnlyStringMap contextData = logEvent.getContextData();
                contextData.forEach((key, value) -> {
                    if (!PowertoolsLoggedFields.stringValues().contains(key)) {
                        jsonWriter.writeSeparator();
                        jsonWriter.writeString(key);
                        stringBuilder.append(':');
                        jsonWriter.writeValue(value);
                    }
                });
            };

    private final EventResolver internalResolver;

    private static final Map<String, EventResolver> eventResolverMap = Stream.of(new Object[][] {
            { SERVICE.getName(), SERVICE_RESOLVER },
            { FUNCTION_NAME.getName(), FUNCTION_NAME_RESOLVER },
            { FUNCTION_VERSION.getName(), FUNCTION_VERSION_RESOLVER },
            { FUNCTION_ARN.getName(), FUNCTION_ARN_RESOLVER },
            { FUNCTION_MEMORY_SIZE.getName(), FUNCTION_MEMORY_RESOLVER },
            { FUNCTION_REQUEST_ID.getName(), FUNCTION_REQ_RESOLVER },
            { FUNCTION_COLD_START.getName(), COLD_START_RESOLVER },
            { FUNCTION_TRACE_ID.getName(), XRAY_TRACE_RESOLVER },
            { SAMPLING_RATE.getName(), SAMPLING_RATE_RESOLVER },
            { "region", REGION_RESOLVER },
            { "account_id", ACCOUNT_ID_RESOLVER },
            { "message", MESSAGE_RESOLVER }
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (EventResolver) data[1]));


    PowertoolsResolver(final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        if (fieldName == null) {
            internalResolver = NON_POWERTOOLS_FIELD_RESOLVER;
        } else {
            internalResolver = eventResolverMap.get(fieldName);
            if (internalResolver == null) {
                throw new IllegalArgumentException("unknown field: " + fieldName);
            }
        }
    }

    @Override
    public void resolve(LogEvent value, JsonWriter jsonWriter) {
        internalResolver.resolve(value, jsonWriter);
    }

    @Override
    public boolean isResolvable(LogEvent value) {
        ReadOnlyStringMap contextData = value.getContextData();
        return null != contextData && !contextData.isEmpty() && internalResolver.isResolvable(value);
    }
}
