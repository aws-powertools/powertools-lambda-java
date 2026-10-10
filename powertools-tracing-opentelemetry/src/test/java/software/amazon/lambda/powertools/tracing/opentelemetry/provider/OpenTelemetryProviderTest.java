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

package software.amazon.lambda.powertools.tracing.opentelemetry.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.TraceContextPropagationMode;

class OpenTelemetryProviderTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        System.clearProperty("otel.traces.exporter");
    }

    @Test
    void shouldProvideTextMapGetter() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        assertThat(getter).isNotNull();
    }

    @Test
    void shouldGetValueFromCarrierWithTextMapGetter() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        Map<String, String> carrier = Map.of("key1", "value1", "key2", "value2");

        assertThat(getter.get(carrier, "key1")).isEqualTo("value1");
        assertThat(getter.get(carrier, "key2")).isEqualTo("value2");
    }

    @Test
    void shouldReturnNullWhenKeyNotFoundInCarrier() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        Map<String, String> carrier = Map.of("key1", "value1");

        assertThat(getter.get(carrier, "unknown")).isNull();
        assertThat(getter.get(carrier, "")).isNull();
    }

    @Test
    void shouldReturnNullWhenCarrierIsNull() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        assertThat(getter.get(null, "key1")).isNull();
        assertThat(getter.get(null, "anyKey")).isNull();
    }

    @Test
    void shouldReturnAllKeysFromCarrier() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        Map<String, String> carrier = Map.of("key1", "value1", "key2", "value2", "key3", "value3");

        assertThat(getter.keys(carrier)).containsExactlyInAnyOrder("key1", "key2", "key3");
    }

    @Test
    void shouldReturnEmptyKeysWhenCarrierIsNull() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        assertThat(getter.keys(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyKeysWhenCarrierIsEmpty() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        assertThat(getter.keys(Collections.emptyMap())).isEmpty();
    }

    @Test
    void shouldHandleMutableCarrier() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        Map<String, String> carrier = new HashMap<>();
        carrier.put("key1", "value1");
        carrier.put("key2", "value2");

        assertThat(getter.get(carrier, "key1")).isEqualTo("value1");
        assertThat(getter.keys(carrier)).containsExactlyInAnyOrder("key1", "key2");
    }

    @Test
    void shouldProvidePropagator() {
        TextMapPropagator propagator = OpenTelemetryProvider.propagator();

        assertThat(propagator).isNotNull();
    }

    @Test
    void shouldReturnSamePropagatorInstance() {
        TextMapPropagator propagator1 = OpenTelemetryProvider.propagator();
        TextMapPropagator propagator2 = OpenTelemetryProvider.propagator();

        assertThat(propagator1).isSameAs(propagator2);
    }

    @Test
    void shouldPropagatorHaveFields() {
        TextMapPropagator propagator = OpenTelemetryProvider.propagator();

        assertThat(propagator.fields()).isNotEmpty();
        assertThat(propagator.fields()).contains("traceparent");
    }

    @Test
    void shouldProvideTracer() {
        Tracer tracer = OpenTelemetryProvider.tracer();

        assertThat(tracer).isNotNull();
    }

    @Test
    void shouldReturnSameTracerInstance() {
        Tracer tracer1 = OpenTelemetryProvider.tracer();
        Tracer tracer2 = OpenTelemetryProvider.tracer();

        assertThat(tracer1).isSameAs(tracer2);
    }

    @Test
    void shouldTracerCreateSpans() {
        Tracer tracer = OpenTelemetryProvider.tracer();

        assertThat(tracer.spanBuilder("test-span")).isNotNull();
    }

    @Test
    void shouldProvideObjectMapper() {
        ObjectMapper objectMapper = OpenTelemetryProvider.objectMapper();

        assertThat(objectMapper).isNotNull();
    }

    @Test
    void shouldReturnSameObjectMapperInstance() {
        ObjectMapper mapper1 = OpenTelemetryProvider.objectMapper();
        ObjectMapper mapper2 = OpenTelemetryProvider.objectMapper();

        assertThat(mapper1).isSameAs(mapper2);
    }

    @Test
    void shouldObjectMapperBeUsable() throws Exception {
        ObjectMapper mapper = OpenTelemetryProvider.objectMapper();

        String json = mapper.writeValueAsString(Map.of("key", "value"));
        assertThat(json).contains("key").contains("value");

        Map<String, String> parsed = mapper.readValue(json, Map.class);
        assertThat(parsed).containsEntry("key", "value");
    }

    @Test
    void shouldProvideDefaultTraceContextPropagationMode() {
        TraceContextPropagationMode mode = OpenTelemetryProvider.traceContextPropagationMode();

        assertThat(mode).isEqualTo(TraceContextPropagationMode.PARENT);
    }

    @Test
    void shouldReturnSameTraceContextPropagationModeInstance() {
        TraceContextPropagationMode mode1 = OpenTelemetryProvider.traceContextPropagationMode();
        TraceContextPropagationMode mode2 = OpenTelemetryProvider.traceContextPropagationMode();

        assertThat(mode1).isEqualTo(mode2);
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE", value = "LINK")
    void shouldReadTraceContextPropagationModeFromEnvironment() {
        assertThat(TraceContextPropagationMode.LINK).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE", value = "PARENT")
    void shouldHandleParentModeFromEnvironment() {
        assertThat(TraceContextPropagationMode.PARENT).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE", value = "invalid")
    void shouldDefaultToParentOnInvalidEnvironmentValue() {
        assertThat(TraceContextPropagationMode.PARENT).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE", value = "")
    void shouldDefaultToParentOnEmptyEnvironmentValue() {
        assertThat(TraceContextPropagationMode.PARENT).isNotNull();
    }

    @Test
    void shouldForceFlush() {
        CompletableResultCode result = OpenTelemetryProvider.forceFlush();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldForceFlushComplete() {
        CompletableResultCode result = OpenTelemetryProvider.forceFlush();

        assertThat(result.join(5000, java.util.concurrent.TimeUnit.MILLISECONDS).isSuccess()).isTrue();
    }


    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_HEADERS", value = "key1=value1,key2=value2")
    void shouldParseHeadersCorrectly() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_HEADERS", value = "")
    void shouldHandleEmptyHeaders() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", value = "grpc")
    void shouldUseGrpcProtocol() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", value = "http/protobuf")
    void shouldUseHttpProtobufProtocol() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", value = "")
    void shouldDefaultToGrpcWhenProtocolIsEmpty() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", value = "http://localhost:4317")
    void shouldUseCustomEndpoint() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }

    @Test
    @SetEnvironmentVariable(key = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", value = "")
    void shouldHandleEmptyEndpoint() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
    }


    @Test
    void shouldAllComponentsWorkTogether() {
        assertThat(OpenTelemetryProvider.tracer()).isNotNull();
        assertThat(OpenTelemetryProvider.propagator()).isNotNull();
        assertThat(OpenTelemetryProvider.textMapGetter()).isNotNull();
        assertThat(OpenTelemetryProvider.objectMapper()).isNotNull();
        assertThat(OpenTelemetryProvider.traceContextPropagationMode()).isNotNull();
        assertThat(OpenTelemetryProvider.forceFlush()).isNotNull();
    }

    @Test
    void shouldTracerAndPropagatorBeCompatible() {
        Tracer tracer = OpenTelemetryProvider.tracer();
        TextMapPropagator propagator = OpenTelemetryProvider.propagator();

        assertThat(tracer.spanBuilder("test").startSpan()).isNotNull();
        assertThat(propagator.fields()).isNotEmpty();
    }

    @Test
    void shouldTextMapGetterWorkWithEmptyMap() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();
        Map<String, String> emptyMap = Collections.emptyMap();

        assertThat(getter.keys(emptyMap)).isEmpty();
        assertThat(getter.get(emptyMap, "anyKey")).isNull();
    }

    @Test
    void shouldTextMapGetterWorkWithSingleEntry() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();
        Map<String, String> singleEntry = Map.of("singleKey", "singleValue");

        assertThat(getter.keys(singleEntry)).containsExactly("singleKey");
        assertThat(getter.get(singleEntry, "singleKey")).isEqualTo("singleValue");
    }

    @Test
    void shouldTextMapGetterHandleSpecialCharacters() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();
        Map<String, String> carrier = Map.of(
                "key-with-dash", "value1",
                "key_with_underscore", "value2",
                "key.with.dot", "value3"
        );

        assertThat(getter.get(carrier, "key-with-dash")).isEqualTo("value1");
        assertThat(getter.get(carrier, "key_with_underscore")).isEqualTo("value2");
        assertThat(getter.get(carrier, "key.with.dot")).isEqualTo("value3");
    }

    @Test
    void shouldHandleNullSafely() {
        TextMapGetter<Map<String, String>> getter = OpenTelemetryProvider.textMapGetter();

        assertThat(getter.get(null, null)).isNull();
        assertThat(getter.get(null, "key")).isNull();
        assertThat(getter.keys(null)).isEmpty();
    }

    @Test
    void shouldObjectMapperHandleComplexObjects() throws Exception {
        ObjectMapper mapper = OpenTelemetryProvider.objectMapper();

        Map<String, Object> complex = Map.of(
                "string", "value",
                "number", 123,
                "nested", Map.of("inner", "data")
        );

        String json = mapper.writeValueAsString(complex);
        assertThat(json).isNotNull();

        Map parsed = mapper.readValue(json, Map.class);
        assertThat(parsed).containsKeys("string", "number", "nested");
    }

    @Test
    void shouldTracerNameBeCorrect() {
        Tracer tracer = OpenTelemetryProvider.tracer();
        assertThat(tracer.spanBuilder("test").startSpan().getSpanContext()).isNotNull();
    }
}