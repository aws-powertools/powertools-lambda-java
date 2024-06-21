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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static software.amazon.lambda.powertools.common.internal.SystemWrapper.getenv;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_ARN;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_MEMORY_SIZE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SAMPLING_RATE;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;

class PowertoolsResolverTest {

    @ParameterizedTest
    @EnumSource(value = PowertoolsLoggedFields.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"FUNCTION_MEMORY_SIZE", "SAMPLING_RATE", "FUNCTION_COLD_START", "CORRELATION_ID"})
    void shouldResolveFunctionStringInfo(PowertoolsLoggedFields field) {
        String result = resolveField(field.getName(), "value");
        assertThat(result).isEqualTo("\"value\"");
    }

    @Test
    void shouldResolveMemorySize() {
        String result = resolveField(FUNCTION_MEMORY_SIZE.getName(), "42");
        assertThat(result).isEqualTo("42");
    }

    @Test
    void shouldResolveSamplingRate() {
        String result = resolveField(SAMPLING_RATE.getName(), "0.4");
        assertThat(result).isEqualTo("0.4");
    }

    @Test
    void shouldResolveColdStart() {
        String result = resolveField(FUNCTION_COLD_START.getName(), "true");
        assertThat(result).isEqualTo("true");
    }

    @Test
    void shouldResolveAccountId() {
        String result = resolveField(FUNCTION_ARN.getName(), "account_id", "arn:aws:lambda:us-east-2:123456789012:function:my-function");
        assertThat(result).isEqualTo("\"123456789012\"");
    }

    @Test
    void unknownField_shouldThrowException() {
        assertThatThrownBy(() -> resolveField("custom-random-unknown-field", "custom-random-unknown-field", "Once apon a time in Switzerland..."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown field: custom-random-unknown-field");
    }

    @Test
    void shouldResolveRegion() {
        try (MockedStatic<SystemWrapper> mocked = mockStatic(SystemWrapper.class)) {
            mocked.when(() -> getenv("AWS_REGION"))
                    .thenReturn("eu-central-2");

            String result = resolveField("region", "dummy, will use the env var");
            assertThat(result).isEqualTo("\"eu-central-2\"");
        }
    }

    private static String resolveField(String field, String value) {
        return resolveField(field, field, value);
    }

    private static String resolveField(String data, String field, String value) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("field", field);

        TemplateResolverConfig config = new TemplateResolverConfig(configMap);
        PowertoolsResolver resolver = new PowertoolsResolver(config);
        JsonWriter writer = JsonWriter
                .newBuilder()
                .setMaxStringLength(1000)
                .setTruncatedStringSuffix("")
                .build();

        StringMap contextMap = new SortedArrayStringMap();
        contextMap.putValue(data, value);

        Log4jLogEvent logEvent = Log4jLogEvent.newBuilder().setContextData(contextMap).build();
        if (resolver.isResolvable(logEvent)) {
            resolver.resolve(logEvent, writer);
        }

        return writer.getStringBuilder().toString();
    }
}
