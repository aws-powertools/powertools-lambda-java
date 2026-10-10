
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

package software.amazon.lambda.powertools.tracing.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanScope;

class TracingOpenTelemetryTest {

    private InMemorySpanExporter exporter;
    private SdkTracerProvider tracerProvider;
    private Tracer tracer;

    public static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    public static final TextMapSetter<Map<String, String>> MAP_SETTER = Map::put;

    @BeforeEach
    void setUp() {
        exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        tracer = tracerProvider.get("test-tracer");
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void testDefaultConstructor() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry();
        
        assertThat(tracing).isNotNull();
        assertThat(tracing.tracer()).isNotNull();
        assertThat(tracing.propagator()).isNotNull();
        assertThat(tracing.eventContextExtractorResolver()).isNotNull();
    }

    @Test
    void testConstructorWithTracer() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThat(tracing.tracer()).isEqualTo(tracer);
        assertThat(tracing.propagator()).isNotNull();
        assertThat(tracing.eventContextExtractorResolver()).isNotNull();
    }

    @Test
    void testCreate() {

        TracingOpenTelemetry tracing = TracingOpenTelemetry.create();

        assertThat(tracing).isNotNull();
        assertThat(tracing.tracer()).isNotNull();
    }

    @Test
    void testBuilder() {

        TracingOpenTelemetry tracing = TracingOpenTelemetry.builder()
                .tracer(tracer)
                .build();

        assertThat(tracing).isNotNull();
        assertThat(tracing.tracer()).isEqualTo(tracer);
    }

    @Test
    void testAddSpanWithName() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope scope = tracing.addSpan("test-span")) {

            assertThat(scope.span().getSpanContext().isValid()).isTrue();
            assertThat(Span.current()).isEqualTo(scope.span());
        }

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getName()).isEqualTo("test-span");
        assertThat(exporter.getFinishedSpanItems().get(0).getKind()).isEqualTo(SpanKind.INTERNAL);
    }

    @Test
    void testAddSpanWithNameAndKind() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope scope = tracing.addSpan("client-span", SpanKind.CLIENT)) {

            assertThat(scope.span().getSpanContext().isValid()).isTrue();
        }

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getKind()).isEqualTo(SpanKind.CLIENT);
    }

    @Test
    void testAddSpanWithAttributes() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Attributes attributes = Attributes.builder()
                .put("key1", "value1")
                .put("key2", 123L)
                .build();

        try (SpanScope scope = tracing.addSpan("span-with-attrs", SpanKind.INTERNAL, attributes)) {
            assertThat(scope.span()).isNotNull();
        }

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getAttributes().get(AttributeKey.stringKey("key1")))
                .isEqualTo("value1");
        assertThat(exporter.getFinishedSpanItems().get(0).getAttributes().get(AttributeKey.longKey("key2")))
                .isEqualTo(123L);
    }

    @Test
    void testAddSpanEndsWhenScopeIsClosed() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope ignored = tracing.addSpan("closing-span")) {
            assertThat(exporter.getFinishedSpanItems()).isEmpty();
        }

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getName()).isEqualTo("closing-span");
    }

    @Test
    void testAddSpanRestoresPreviousSpan() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope outer = tracing.addSpan("outer")) {
            assertThat(Span.current()).isEqualTo(outer.span());

            try (SpanScope inner = tracing.addSpan("inner")) {
                assertThat(Span.current()).isEqualTo(inner.span());
            }

            assertThat(Span.current()).isEqualTo(outer.span());
        }

        assertThat(exporter.getFinishedSpanItems()).hasSize(2);
    }

    @Test
    void testAddSpanWithSpanContextLinks() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        SpanContext linkContext1 = SpanContext.createFromRemoteParent(
                "00000000000000000000000000000001",
                "0000000000000001",
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
        );

        SpanContext linkContext2 = SpanContext.createFromRemoteParent(
                "00000000000000000000000000000002",
                "0000000000000002",
                io.opentelemetry.api.trace.TraceFlags.getSampled(),
                io.opentelemetry.api.trace.TraceState.getDefault()
        );

        try (SpanScope scope = tracing.addSpan(
                "span-with-links",
                SpanKind.INTERNAL,
                Attributes.empty(),
                Context.current(),
                Arrays.asList(linkContext1, linkContext2)
        )) {
            assertThat(scope.span()).isNotNull();
        }

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getLinks()).hasSize(2);
    }

    @Test
    void testWithSpan() throws Exception {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        String result = tracing.withSpan("operation-span", span -> {
            assertThat(span).isNotNull();
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getName()).isEqualTo("operation-span");
    }

    @Test
    void testWithSpanRecordsException() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        RuntimeException exception = new RuntimeException("boom");

        assertThatThrownBy(() ->
                tracing.withSpan("failing-span", span -> {
                    throw exception;
                })
        ).isSameAs(exception);

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getEvents()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getEvents().get(0).getName())
                .isEqualTo("exception");
        assertThat(exporter.getFinishedSpanItems().get(0).getStatus().getStatusCode())
                .isEqualTo(io.opentelemetry.api.trace.StatusCode.ERROR);
    }

    @Test
    void testWithSpanWithKindAndAttributes() throws Exception {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Attributes attributes = Attributes.builder()
                .put("custom", "attribute")
                .build();

        Integer result = tracing.withSpan("custom-span", SpanKind.SERVER, attributes, span -> 42);

        assertThat(result).isEqualTo(42);
        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
        assertThat(exporter.getFinishedSpanItems().get(0).getKind()).isEqualTo(SpanKind.SERVER);
    }

    @Test
    void testCurrentSpan() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope scope = tracing.addSpan("current-test")) {
            Span current = tracing.currentSpan();

            assertThat(current).isEqualTo(scope.span());
        }
    }

    @Test
    void testExtractContext() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        Context context = tracing.extractContext(headers, MAP_GETTER);
        SpanContext spanContext = Span.fromContext(context).getSpanContext();

        assertThat(spanContext.isValid()).isTrue();
        assertThat(spanContext.isRemote()).isTrue();
        assertThat(spanContext.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(spanContext.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
    }

    @Test
    void testExtractContextWithParentContext() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        Context parentContext = Context.current();

        Context context = tracing.extractContext(parentContext, headers, MAP_GETTER);
        SpanContext spanContext = Span.fromContext(context).getSpanContext();

        assertThat(spanContext.isValid()).isTrue();
    }

    @Test
    void testExtractContextWithMissingTraceparent() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> headers = new HashMap<>();

        Context context = tracing.extractContext(headers, MAP_GETTER);

        assertThat(Span.fromContext(context).getSpanContext().isValid()).isFalse();
    }

    @Test
    void testInjectContext() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> carrier = new HashMap<>();

        try (SpanScope scope = tracing.addSpan("inject-test")) {
            tracing.injectContext(carrier, MAP_SETTER);
        }

        assertThat(carrier).containsKey("traceparent");
    }

    @Test
    void testInjectContextWithSpecificContext() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> carrier = new HashMap<>();

        try (SpanScope scope = tracing.addSpan("inject-test")) {
            Context context = Context.current();
            tracing.injectContext(context, carrier, MAP_SETTER);
        }

        assertThat(carrier).containsKey("traceparent");
    }

    @Test
    void testFlush() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope ignored = tracing.addSpan("flush-test")) {
            // span created
        }

        tracing.flush();

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
    }

    @Test
    void testFlushWithTimeout() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope ignored = tracing.addSpan("flush-timeout-test")) {
            // span created
        }

        tracing.flush(10, TimeUnit.SECONDS);

        assertThat(exporter.getFinishedSpanItems()).hasSize(1);
    }

    @Test
    void testBuilderWithCustomPropagator() {

        TextMapPropagator customPropagator = TextMapPropagator.noop();

        TracingOpenTelemetry tracing = TracingOpenTelemetry.builder()
                .tracer(tracer)
                .propagator(customPropagator)
                .build();

        assertThat(tracing.propagator()).isEqualTo(customPropagator);
    }

    @Test
    void testAddSpanThrowsNullPointerExceptionForNullName() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.addSpan(null, SpanKind.INTERNAL, Attributes.empty(), Context.current(), Collections.emptyList())
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name must not be null");
    }

    @Test
    void testAddSpanThrowsNullPointerExceptionForNullKind() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.addSpan("test", null, Attributes.empty(), Context.current(), Collections.emptyList())
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("kind must not be null");
    }

    @Test
    void testAddSpanThrowsNullPointerExceptionForNullAttributes() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.addSpan("test", SpanKind.INTERNAL, null, Context.current(), Collections.emptyList())
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("attributes must not be null");
    }

    @Test
    void testAddSpanThrowsNullPointerExceptionForNullParentContext() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.addSpan("test", SpanKind.INTERNAL, Attributes.empty(), null, Collections.emptyList())
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parentContext must not be null");
    }

    @Test
    void testAddSpanThrowsNullPointerExceptionForNullSpanContexts() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.addSpan("test", SpanKind.INTERNAL, Attributes.empty(), Context.current(), null)
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("spanContexts must not be null");
    }

    @Test
    void testWithSpanThrowsNullPointerExceptionForNullOperation() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.withSpan("test", SpanKind.INTERNAL, Attributes.empty(), null)
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("operation must not be null");
    }

    @Test
    void testExtractContextThrowsNullPointerExceptionForNullCarrier() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.extractContext(Context.current(), null, MAP_GETTER)
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("carrier must not be null");
    }

    @Test
    void testExtractContextThrowsNullPointerExceptionForNullGetter() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> carrier = new HashMap<>();

        assertThatThrownBy(() ->
                tracing.extractContext(Context.current(), carrier, null)
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("getter must not be null");
    }

    @Test
    void testInjectContextThrowsNullPointerExceptionForNullCarrier() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        assertThatThrownBy(() ->
                tracing.injectContext(Context.current(), null, MAP_SETTER)
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("carrier must not be null");
    }

    @Test
    void testInjectContextThrowsNullPointerExceptionForNullSetter() {

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);
        Map<String, String> carrier = new HashMap<>();

        assertThatThrownBy(() ->
                tracing.injectContext(Context.current(), carrier, null)
        ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("setter must not be null");
    }
}