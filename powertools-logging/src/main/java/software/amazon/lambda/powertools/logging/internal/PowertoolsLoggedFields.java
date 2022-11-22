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
package software.amazon.lambda.powertools.logging.internal;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PowertoolsLoggedFields {
    FUNCTION_NAME("function_name"),
    FUNCTION_VERSION("function_version"),
    FUNCTION_ARN("function_arn"),
    FUNCTION_MEMORY_SIZE("function_memory_size"),
    FUNCTION_REQUEST_ID("function_request_id"),
    FUNCTION_COLD_START("cold_start"),
    FUNCTION_TRACE_ID("xray_trace_id"),
    SAMPLING_RATE("sampling_rate"),
    SERVICE("service");

    private final String name;

    PowertoolsLoggedFields(String name) {
        this.name = name;
    }

    public static List<String> stringValues() {
        return Stream.of(values()).map(PowertoolsLoggedFields::getName).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    static Map<String, String> setValuesFromLambdaContext(Context context) {
        Map<String, String> hashMap = new HashMap<>();

        hashMap.put(FUNCTION_NAME.name, context.getFunctionName());
        hashMap.put(FUNCTION_VERSION.name, context.getFunctionVersion());
        hashMap.put(FUNCTION_ARN.name, context.getInvokedFunctionArn());
        hashMap.put(FUNCTION_MEMORY_SIZE.name, String.valueOf(context.getMemoryLimitInMB()));
        hashMap.put(FUNCTION_REQUEST_ID.name, String.valueOf(context.getAwsRequestId()));

        return hashMap;
    }
}
