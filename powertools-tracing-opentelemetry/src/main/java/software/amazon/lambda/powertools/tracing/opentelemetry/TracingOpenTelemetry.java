/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.tracing.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.LambdaEventContextExtractorResolver;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanOperation;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanScope;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;


public final class TracingOpenTelemetry {

    private static final TracingOpenTelemetry DEFAULT_INSTANCE = new TracingOpenTelemetry();
    private final Tracer tracer;
    private final TextMapPropagator propagator;
    private final LambdaEventContextExtractorResolver eventContextExtractorResolver;

    private TracingOpenTelemetry(Builder builder) {
        this.tracer = Objects.requireNonNull(builder.tracer, "tracer must not be null");
        this.propagator = Objects.requireNonNull(builder.propagator, "propagator must not be null");
        this.eventContextExtractorResolver = Objects.requireNonNull(
                builder.eventContextExtractorResolver,
                "eventContextExtractorResolver must not be null"
        );
    }

    public TracingOpenTelemetry() {
        this(OpenTelemetryProvider.tracer());
    }


    public TracingOpenTelemetry(Tracer tracer) {
        this(tracer, createDefaultPropagator(), createDefaultEventContextExtractorResolver());
    }


    public TracingOpenTelemetry(
            Tracer tracer,
            TextMapPropagator propagator,
            LambdaEventContextExtractorResolver eventContextExtractorResolver
    ) {

        this.tracer = Objects.requireNonNull(tracer, "tracer must not be null");
        this.propagator = Objects.requireNonNull(propagator, "propagator must not be null");
        this.eventContextExtractorResolver = Objects.requireNonNull(
                eventContextExtractorResolver,
                "eventContextExtractorResolver must not be null"
        );
    }

    public Tracer tracer() {
        return tracer;
    }

    public TextMapPropagator propagator() {
        return propagator;
    }

    public LambdaEventContextExtractorResolver eventContextExtractorResolver() {
        return eventContextExtractorResolver;
    }

    public Span currentSpan() {
        return Span.current();
    }

    public CompletableResultCode flush() {
        return flush(5, TimeUnit.SECONDS);
    }

    public CompletableResultCode flush(long timeout, TimeUnit unit) {
        return OpenTelemetryProvider.forceFlush().join(timeout, unit);
    }

    public SpanScope addSpan(String name) {
        return addSpan(name, SpanKind.INTERNAL);
    }


    public SpanScope addSpan(String name, SpanKind kind) {

        return addSpan(name, kind, Attributes.empty());
    }

    public SpanScope addSpan(String name, SpanKind kind, Attributes attributes) {

        return addSpan(name, kind, attributes, Context.current());
    }

    public SpanScope addSpan(String name, SpanKind kind, Attributes attributes, Context parentContext) {

        return addSpan(name, kind, attributes, parentContext, Collections.emptyList());
    }

    public SpanScope addSpan(
            String name,
            SpanKind kind,
            Attributes attributes,
            Context parentContext,
            List<SpanContext> spanContexts
    ) {

        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");
        Objects.requireNonNull(parentContext, "parentContext must not be null");
        Objects.requireNonNull(spanContexts, "spanContexts must not be null");

        SpanBuilder spanBuilder = tracer
                .spanBuilder(name)
                .setSpanKind(kind)
                .setParent(parentContext)
                .setAllAttributes(attributes);

        spanContexts.forEach(spanBuilder::addLink);

        return new SpanScope(spanBuilder.startSpan());
    }


    public <T> T withSpan(String name, SpanOperation<T> operation) throws Exception {

        return withSpan(name, SpanKind.INTERNAL, Attributes.empty(), operation);
    }

    public <T> T withSpan(
            String name,
            SpanKind kind,
            Attributes attributes,
            SpanOperation<T> operation
    ) throws Exception {
        Objects.requireNonNull(operation, "operation must not be null");

        try (SpanScope scope = addSpan(name, kind, attributes)) {
            try {
                return operation.execute(scope.span());
            } catch (Exception exception) {
                scope.recordException(exception);
                throw exception;
            }
        }
    }

    public <T> Context extractContext(T carrier, TextMapGetter<T> getter) {

        return extractContext(Context.current(), carrier, getter);
    }

    public <T> Context extractContext(Context context, T carrier, TextMapGetter<T> getter) {

        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(carrier, "carrier must not be null");
        Objects.requireNonNull(getter, "getter must not be null");

        return propagator.extract(context, carrier, getter);
    }

    public <T> void injectContext(T carrier, TextMapSetter<T> setter) {

        injectContext(Context.current(), carrier, setter);
    }

    public <T> void injectContext(Context context, T carrier, TextMapSetter<T> setter) {

        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(carrier, "carrier must not be null");
        Objects.requireNonNull(setter, "setter must not be null");

        propagator.inject(context, carrier, setter);
    }

    private static TextMapPropagator createDefaultPropagator() {
        return OpenTelemetryProvider.propagator();
    }

    private static LambdaEventContextExtractorResolver createDefaultEventContextExtractorResolver() {
        return LambdaEventContextExtractorResolver.create();
    }

    public static TracingOpenTelemetry create() {
        return DEFAULT_INSTANCE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Tracer tracer;
        private TextMapPropagator propagator = createDefaultPropagator();
        private LambdaEventContextExtractorResolver eventContextExtractorResolver =
                createDefaultEventContextExtractorResolver();

        public Builder tracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        public Builder propagator(TextMapPropagator propagator) {
            this.propagator = propagator;
            return this;
        }

        public Builder eventContextExtractorResolver(
                LambdaEventContextExtractorResolver eventContextExtractorResolver) {
            this.eventContextExtractorResolver = eventContextExtractorResolver;
            return this;
        }

        public TracingOpenTelemetry build() {
            return new TracingOpenTelemetry(this);
        }
    }
}