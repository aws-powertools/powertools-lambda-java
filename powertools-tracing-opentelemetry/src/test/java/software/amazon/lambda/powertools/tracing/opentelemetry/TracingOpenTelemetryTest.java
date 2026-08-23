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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanScope;

class TracingOpenTelemetryTest {

    public static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(
                Map<String, String> carrier,
                String key) {
            return carrier.get(key);
        }
    };

    @Test
    void shouldCreateAndMakeSpanCurrent() {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();

        Tracer tracer = tracerProvider.get("test-tracer");

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope scope = tracing.addSpan("payment")) {
            assertThat(scope.span().getSpanContext().isValid())
                    .isTrue();

            assertThat(Span.current())
                    .isEqualTo(scope.span());
        }
    }

    @Test
    void shouldEndSpanWhenScopeIsClosed() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();

        Tracer tracer = tracerProvider.get("test-tracer");

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope ignored = tracing.addSpan("payment")) {
            assertThat(exporter.getFinishedSpanItems())
                    .isEmpty();
        }

        assertThat(exporter.getFinishedSpanItems())
                .hasSize(1);

        assertThat(exporter.getFinishedSpanItems().get(0).getName())
                .isEqualTo("payment");

        tracerProvider.close();
    }

    @Test
    void shouldRestorePreviousSpanWhenScopeIsClosed() {

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();

        Tracer tracer = tracerProvider.get("test-tracer");

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        try (SpanScope outer = tracing.addSpan("outer")) {

            assertThat(Span.current()).isEqualTo(outer.span());

            try (SpanScope inner = tracing.addSpan("inner")) {
                assertThat(Span.current()).isEqualTo(inner.span());
            }

            assertThat(Span.current()).isEqualTo(outer.span());
        }
    }

    @Test
    void shouldRecordException() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();

        Tracer tracer = tracerProvider.get("test-tracer");

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        RuntimeException exception = new RuntimeException("boom");

        try (SpanScope scope = tracing.addSpan("payment")) {
            scope.recordException(exception);
        }

        assertThat(exporter.getFinishedSpanItems())
                .hasSize(1);

        assertThat(exporter.getFinishedSpanItems().get(0).getEvents())
                .hasSize(1);

        assertThat(exporter.getFinishedSpanItems().get(0).getEvents().get(0).getName())
                .isEqualTo("exception");

        assertThat(exporter.getFinishedSpanItems().get(0).getStatus().getStatusCode())
                .isEqualTo(io.opentelemetry.api.trace.StatusCode.ERROR);

        tracerProvider.close();
    }

    @Test
    void shouldRecordExceptionWhenUsingWithSpan() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();

        Tracer tracer = tracerProvider.get("test-tracer");

        TracingOpenTelemetry tracing = new TracingOpenTelemetry(tracer);

        RuntimeException exception = new RuntimeException("boom");

        assertThatThrownBy(() ->
                tracing.withSpan("payment", span -> {
                    throw exception;
                })
        ).isSameAs(exception);

        assertThat(exporter.getFinishedSpanItems())
                .hasSize(1);

        assertThat(exporter.getFinishedSpanItems().get(0).getEvents())
                .hasSize(1);

        assertThat(exporter.getFinishedSpanItems().get(0).getEvents().get(0).getName())
                .isEqualTo("exception");

        assertThat(exporter.getFinishedSpanItems().get(0).getStatus().getStatusCode())
                .isEqualTo(io.opentelemetry.api.trace.StatusCode.ERROR);

        tracerProvider.close();
    }

    @Test
    void shouldExtractContext() {
        TextMapPropagator propagator =
                W3CTraceContextPropagator.getInstance();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();

        Tracer tracer = tracerProvider.get("test-tracer");

        TracingOpenTelemetry tracing =
                TracingOpenTelemetry.builder()
                        .tracer(tracer)
                        .propagator(propagator)
                        .build();

        Map<String, String> headers = new HashMap<>();
        headers.put(
                "traceparent",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );

        Context context = tracing.extractContext(
                headers,
                MAP_GETTER
        );

        SpanContext spanContext = Span.fromContext(context).getSpanContext();

        assertThat(spanContext.isValid()).isTrue();
        assertThat(spanContext.isRemote()).isTrue();

        assertThat(spanContext.getTraceId())
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");

        assertThat(spanContext.getSpanId())
                .isEqualTo("00f067aa0ba902b7");

        assertThat(spanContext.getTraceFlags().isSampled())
                .isTrue();
    }

    @Test
    void shouldReturnInvalidContextWhenTraceparentIsMissing() {
        TextMapPropagator propagator =
                W3CTraceContextPropagator.getInstance();

        SdkTracerProvider tracerProvider =
                SdkTracerProvider.builder().build();

        TracingOpenTelemetry tracing =
                TracingOpenTelemetry.builder()
                        .tracer(tracerProvider.get("test-tracer"))
                        .propagator(propagator)
                        .build();

        Map<String, String> headers = new HashMap<>();

        Context context = tracing.extractContext(
                headers,
                MAP_GETTER
        );

        assertThat(Span.fromContext(context).getSpanContext().isValid())
                .isFalse();
    }


}