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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolver;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolverConfig;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

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
                        logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_NAME.getName());
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
            return null != samplingRate;
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String samplingRate =
                    logEvent.getContextData().getValue(PowertoolsLoggedFields.SAMPLING_RATE.getName());
            jsonWriter.writeNumber(Float.parseFloat(samplingRate));
        }
    };

    private static final EventResolver XRAY_TRACE_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String traceId =
                        logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_TRACE_ID.getName());
                jsonWriter.writeString(traceId);
            };

    private static final EventResolver SERVICE_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String service = logEvent.getContextData().getValue(PowertoolsLoggedFields.SERVICE.getName());
                jsonWriter.writeString(service);
            };

    private static final EventResolver REGION_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) ->
                    jsonWriter.writeString(System.getenv("AWS_REGION"));

    private static final EventResolver ACCOUNT_ID_RESOLVER = new EventResolver() {
        @Override
        public boolean isResolvable(LogEvent logEvent) {
            final String arn = logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_ARN.getName());
            return null != arn && !arn.isEmpty();
        }

        @Override
        public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
            final String arn = logEvent.getContextData().getValue(PowertoolsLoggedFields.FUNCTION_ARN.getName());
            jsonWriter.writeString(arn.split(":")[4]);
        }
    };

    private static final EventResolver NON_POWERTOOLS_FIELD_RESOLVER =
            (LogEvent logEvent, JsonWriter jsonWriter) -> {
                StringBuilder stringBuilder = jsonWriter.getStringBuilder();
                // remove dummy field to kick inn powertools resolver
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

    PowertoolsResolver(final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        if (fieldName == null) {
            internalResolver = NON_POWERTOOLS_FIELD_RESOLVER;
        } else {
            switch (fieldName) {
                case "service":
                    internalResolver = SERVICE_RESOLVER;
                    break;
                case "function_name":
                    internalResolver = FUNCTION_NAME_RESOLVER;
                    break;
                case "function_version":
                case "service_version":
                    internalResolver = FUNCTION_VERSION_RESOLVER;
                    break;
                case "function_arn":
                    internalResolver = FUNCTION_ARN_RESOLVER;
                    break;
                case "function_memory_size":
                    internalResolver = FUNCTION_MEMORY_RESOLVER;
                    break;
                case "function_request_id":
                    internalResolver = FUNCTION_REQ_RESOLVER;
                    break;
                case "cold_start":
                    internalResolver = COLD_START_RESOLVER;
                    break;
                case "xray_trace_id":
                    internalResolver = XRAY_TRACE_RESOLVER;
                    break;
                case "region":
                    internalResolver = REGION_RESOLVER;
                    break;
                case "account_id":
                    internalResolver = ACCOUNT_ID_RESOLVER;
                    break;
                case "sampling_rate":
                    internalResolver = SAMPLING_RATE_RESOLVER;
                    break;
                default:
                    throw new IllegalArgumentException("unknown field: " + fieldName);
            }
        }
    }

    static String getName() {
        return "powertools";
    }

    @Override
    public void resolve(LogEvent value, JsonWriter jsonWriter) {
        internalResolver.resolve(value, jsonWriter);
    }

    @Override
    public boolean isResolvable(LogEvent value) {
        ReadOnlyStringMap contextData = value.getContextData();
        return null != contextData && !contextData.isEmpty() && internalResolver.isResolvable();
    }
}
