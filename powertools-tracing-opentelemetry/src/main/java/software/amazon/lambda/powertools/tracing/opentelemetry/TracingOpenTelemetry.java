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
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Objects;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.OpenTelemetryProvider;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanOperation;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanScope;


public final class TracingOpenTelemetry {

    private final Tracer tracer;
    private final TextMapPropagator propagator;

    private TracingOpenTelemetry(Builder builder) {
        this.tracer = Objects.requireNonNull(
                builder.tracer,
                "tracer must not be null"
        );
        this.propagator = Objects.requireNonNull(
                builder.propagator,
                "propagator must not be null"
        );
    }

    public TracingOpenTelemetry() {
        this(OpenTelemetryProvider.tracer());
    }


    public TracingOpenTelemetry(Tracer tracer) {
        this(tracer, createDefaultPropagator());
    }


    public TracingOpenTelemetry(
            Tracer tracer,
            TextMapPropagator propagator) {

        this.tracer = Objects.requireNonNull(
                tracer,
                "tracer must not be null"
        );
        this.propagator = Objects.requireNonNull(
                propagator,
                "propagator must not be null"
        );
    }


    public SpanScope addSpan(String name) {
        return addSpan(name, SpanKind.INTERNAL);
    }


    public SpanScope addSpan(
            String name,
            SpanKind kind) {

        return addSpan(name, kind, Attributes.empty());
    }


    public SpanScope addSpan(
            String name,
            SpanKind kind,
            Attributes attributes) {

        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");

        Span span = tracer.spanBuilder(name)
                .setSpanKind(kind)
                .setAllAttributes(attributes)
                .startSpan();

        return new SpanScope(span);
    }

    public SpanScope addSpan(
            String name,
            SpanKind kind,
            Attributes attributes,
            Context parentContext) {

        Objects.requireNonNull(parentContext, "parentContext must not be null");

        Span span = tracer.spanBuilder(name)
                .setParent(parentContext)
                .setSpanKind(kind)
                .setAllAttributes(attributes)
                .startSpan();

        return new SpanScope(span);
    }


    public Span currentSpan() {
        return Span.current();
    }


    public <T> T withSpan(
            String name,
            SpanOperation<T> operation) throws Exception {

        Objects.requireNonNull(operation, "operation must not be null");

        try (SpanScope scope = addSpan(name)) {
            try {
                return operation.execute(scope.span());
            } catch (Exception exception) {
                scope.recordException(exception);
                throw exception;
            }
        }
    }

    public <T> T captureLambdaHandler(
            String name,
            com.amazonaws.services.lambda.runtime.Context lambdaContext,
            io.opentelemetry.context.Context parentContext,
            SpanOperation<T> operation) throws Exception {

        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(parentContext, "parentContext must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        Span span = tracer.spanBuilder(name)
                .setParent(parentContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("faas.coldstart", LambdaHandlerProcessor.isColdStart())
                .setAttribute("faas.invocation_id", lambdaContext.getAwsRequestId())
                .startSpan();

        try (SpanScope scope = new SpanScope(span)) {
            try {
                T result = operation.execute(span);

                LambdaHandlerProcessor.coldStartDone();

                return result;
            } catch (Exception exception) {
                scope.recordException(exception);
                throw exception;
            }
        }
    }

    public <T> Context extractContext(
            T carrier,
            TextMapGetter<T> getter) {

        return extractContext(Context.current(), carrier, getter);
    }


    public <T> Context extractContext(
            Context context,
            T carrier,
            TextMapGetter<T> getter) {

        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(getter, "getter must not be null");

        return propagator.extract(
                context,
                carrier,
                getter
        );
    }


    public <T> void injectContext(
            T carrier,
            TextMapSetter<T> setter) {

        injectContext(Context.current(), carrier, setter);
    }


    public <T> void injectContext(
            Context context,
            T carrier,
            TextMapSetter<T> setter) {

        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(setter, "setter must not be null");

        propagator.inject(
                context,
                carrier,
                setter
        );
    }

    private static TextMapPropagator createDefaultPropagator() {
        return OpenTelemetryProvider.propagator();
    }


    public static TracingOpenTelemetry create() {
        return new TracingOpenTelemetry();
    }


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Tracer tracer;
        private TextMapPropagator propagator = createDefaultPropagator();

        public Builder tracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        public Builder propagator(TextMapPropagator propagator) {
            this.propagator = propagator;
            return this;
        }

        public TracingOpenTelemetry build() {
            return new TracingOpenTelemetry(this);
        }
    }
}