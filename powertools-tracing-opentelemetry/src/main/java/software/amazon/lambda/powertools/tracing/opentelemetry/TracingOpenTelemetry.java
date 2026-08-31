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

/**
 * A utility class responsible for managing OpenTelemetry tracing functionality,
 * including creating and managing spans, handling context propagation, and facilitating
 * relevant operations for distributed tracing.
 * <p>
 * This class provides methods to manage the life cycle of spans, propagate and extract
 * context, flush telemetry data, and execute operations within spans. It also supports
 * configuration via a builder pattern.
 * <p>
 * The class is designed to be thread-safe and offers a default singleton instance
 * for convenience.
 */
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

    /**
     * Provides access to the current Tracer instance.
     *
     * @return the Tracer instance associated with the current context
     */
    public Tracer tracer() {
        return tracer;
    }

    /**
     * Provides the current TextMapPropagator instance.
     *
     * @return the TextMapPropagator instance used for propagating context information.
     */
    public TextMapPropagator propagator() {
        return propagator;
    }

    /**
     * Retrieves the instance of LambdaEventContextExtractorResolver.
     *
     * @return the resolver used to extract context from Lambda events.
     */
    public LambdaEventContextExtractorResolver eventContextExtractorResolver() {
        return eventContextExtractorResolver;
    }

    /**
     * Retrieves the current active span within the context.
     *
     * @return the currently active span, or null if there is no active span
     */
    public Span currentSpan() {
        return Span.current();
    }

    /**
     * Forces all pending spans and related telemetry data to be processed and exported.
     * This method sends the pending data using the default timeout period.
     *
     * @return a {@code CompletableResultCode} indicating the success or failure of the flush operation
     */
    public CompletableResultCode flush() {
        return flush(5, TimeUnit.SECONDS);
    }

    /**
     * Forces all pending spans and related telemetry data to be processed and exported
     * within a specified timeout period.
     *
     * @param timeout the maximum duration to wait for the flush operation to complete
     * @param unit    the time unit of the {@code timeout} parameter
     * @return a {@code CompletableResultCode} indicating the success or failure of the flush operation
     */
    public CompletableResultCode flush(long timeout, TimeUnit unit) {
        return OpenTelemetryProvider.forceFlush().join(timeout, unit);
    }

    /**
     * Starts a new OpenTelemetry span with the given name and a default {@link SpanKind#INTERNAL} kind.
     *
     * @param name the name of the span to be created
     * @return a {@link SpanScope} instance that manages the lifecycle of the span and its associated context
     */
    public SpanScope addSpan(String name) {
        return addSpan(name, SpanKind.INTERNAL);
    }

    /**
     * Starts a new OpenTelemetry span with the given name, kind, and default attributes.
     *
     * @param name the name of the span to be created
     * @param kind the kind of the span, e.g., {@link SpanKind#INTERNAL}, {@link SpanKind#CLIENT}, etc.
     * @return a {@link SpanScope} instance that manages the lifecycle of the span and its associated context
     */
    public SpanScope addSpan(String name, SpanKind kind) {

        return addSpan(name, kind, Attributes.empty());
    }

    /**
     * Starts a new OpenTelemetry span with the given name, kind, and attributes,
     * using the current thread context as the parent context.
     *
     * @param name       the name of the span to be created
     * @param kind       the kind of the span, such as {@code SpanKind.INTERNAL}, {@code SpanKind.CLIENT}, etc.
     * @param attributes the attributes to associate with the span
     * @return a {@code SpanScope} instance that manages the lifecycle of the span and its associated context
     */
    public SpanScope addSpan(String name, SpanKind kind, Attributes attributes) {

        return addSpan(name, kind, attributes, Context.current());
    }

    /**
     * Starts a new OpenTelemetry span with the given name, kind, attributes, and parent context.
     * The span is returned encapsulated in a {@code SpanScope}, which manages the lifecycle
     * of the span and its associated context.
     *
     * @param name          the name of the span to be created
     * @param kind          the type of the span, such as {@code SpanKind.INTERNAL}, {@code SpanKind.CLIENT}, etc.
     * @param attributes    the attributes to associate with the span
     * @param parentContext the parent context to use for the span
     * @return a {@code SpanScope} instance that manages the lifecycle of the span and its related context
     */
    public SpanScope addSpan(String name, SpanKind kind, Attributes attributes, Context parentContext) {

        return addSpan(name, kind, attributes, parentContext, Collections.emptyList());
    }

    /**
     * Starts a new OpenTelemetry span with the given configuration, including name, kind, attributes,
     * parent context, and links to other spans represented by their {@code SpanContext}s.
     * The resulting span is encapsulated within a {@code SpanScope} for proper lifecycle management.
     *
     * @param name          the name of the span to be created; must not be null
     * @param kind          the type of the span, such as {@code SpanKind.INTERNAL}, {@code SpanKind.CLIENT}, etc.;
     *                      must not be null
     * @param attributes    the attributes to associate with the span; must not be null
     * @param parentContext the parent context to use for the span; must not be null
     * @param spanContexts  the list of {@code SpanContext} instances to link to the created span; must not be null
     * @return a {@code SpanScope} instance that manages the lifecycle of the span and its associated context
     * @throws NullPointerException if any of the parameters are null
     */
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

    /**
     * Executes a given operation within the context of an OpenTelemetry span with the specified name.
     * The span is created with the default {@link SpanKind#INTERNAL} and no additional attributes.
     * Any exceptions thrown during the operation will be recorded in the span.
     *
     * @param <T>       the type of result returned by the operation
     * @param name      the name of the span to be created; must not be null
     * @param operation the operation to execute within the span context; must not be null
     * @return the result of the operation
     * @throws Exception if an error occurs during the execution of the operation
     */
    public <T> T withSpan(String name, SpanOperation<T> operation) throws Exception {

        return withSpan(name, SpanKind.INTERNAL, Attributes.empty(), operation);
    }

    /**
     * Executes a given operation within the context of an OpenTelemetry span
     * with the specified name, kind, and attributes. The span is created and
     * managed within the method. Any exceptions thrown during the operation
     * are recorded in the span before being propagated.
     *
     * @param <T>        the type of result returned by the operation
     * @param name       the name of the span to be created; must not be null
     * @param kind       the kind of the span, such as {@code SpanKind.INTERNAL}
     *                   or {@code SpanKind.CLIENT}; must not be null
     * @param attributes the attributes to associate with the span; must not be null
     * @param operation  the operation to execute within the span context; must not be null
     * @return the result of the operation
     * @throws Exception if an error occurs during the execution of the operation
     */
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

    /**
     * Extracts a {@code Context} from the given carrier using the specified {@link TextMapGetter}.
     *
     * @param <T>     the type of the carrier from which the context is extracted
     * @param carrier the carrier object that holds context propagation data; must not be null
     * @param getter  the {@link TextMapGetter} used to read propagation fields from the carrier; must not be null
     * @return the extracted {@code Context}, or the current context if no context could be extracted
     * @throws NullPointerException if the carrier or getter is null
     */
    public <T> Context extractContext(T carrier, TextMapGetter<T> getter) {

        return extractContext(Context.current(), carrier, getter);
    }

    /**
     * Extracts a {@link Context} from the given carrier using the specified {@link TextMapGetter}.
     *
     * @param context the initial {@link Context} used as the baseline for extraction; must not be null
     * @param carrier the carrier of the propagation fields; must not be null
     * @param getter  the {@link TextMapGetter} used to read propagation fields from the carrier; must not be null
     * @return the extracted {@link Context} containing the propagated values
     */
    public <T> Context extractContext(Context context, T carrier, TextMapGetter<T> getter) {

        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(carrier, "carrier must not be null");
        Objects.requireNonNull(getter, "getter must not be null");

        return propagator.extract(context, carrier, getter);
    }

    /**
     * Injects the current context into the specified carrier using the provided TextMapSetter.
     *
     * @param <T>     The type of the carrier into which the context will be injected.
     * @param carrier The carrier object that will hold the injected context.
     * @param setter  The TextMapSetter implementation used to set the context into the carrier.
     */
    public <T> void injectContext(T carrier, TextMapSetter<T> setter) {

        injectContext(Context.current(), carrier, setter);
    }

    /**
     * Injects the provided {@code Context} into the specified carrier using the given {@code TextMapSetter}.
     *
     * @param context the context to inject; must not be null
     * @param carrier the carrier into which the context will be injected; must not be null
     * @param setter  the {@code TextMapSetter} used to define how the context is set on the carrier; must not be null
     * @param <T>     the type of the carrier
     */
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

    /**
     * Creates and returns the default instance of the TracingOpenTelemetry.
     *
     * @return The default instance of TracingOpenTelemetry.
     */
    public static TracingOpenTelemetry create() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Creates and returns a new instance of the Builder.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating instances of TracingOpenTelemetry.
     * This class provides a fluent API for configuring and constructing
     * a TracingOpenTelemetry object.
     * <p>
     * The Builder allows customization of the following components:
     * - Tracer: A tracer instance used for tracing operations.
     * - TextMapPropagator: A propagator responsible for context propagation.
     * - LambdaEventContextExtractorResolver: A resolver for extracting context from Lambda events.
     */
    public static final class Builder {

        private Tracer tracer;
        private TextMapPropagator propagator = createDefaultPropagator();
        private LambdaEventContextExtractorResolver eventContextExtractorResolver =
                createDefaultEventContextExtractorResolver();

        /**
         * Sets the tracer instance to be used for tracing operations.
         * This method allows specifying a custom tracer, which will
         * be used to create and manage spans in tracing contexts.
         *
         * @param tracer the tracer instance to be used for tracing
         * @return the updated Builder instance for method chaining
         */
        public Builder tracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        /**
         * Sets the {@link TextMapPropagator} to be used for context propagation.
         * This allows specifying a custom propagator to handle the injection and extraction
         * of context data across process boundaries.
         *
         * @param propagator the {@link TextMapPropagator} instance to be used for context propagation
         * @return the updated Builder instance for method chaining
         */
        public Builder propagator(TextMapPropagator propagator) {
            this.propagator = propagator;
            return this;
        }

        /**
         * Sets the {@link LambdaEventContextExtractorResolver} to be used for extracting
         * context from AWS Lambda events. This allows specifying a custom resolver
         * to handle the extraction of trace context from various types of AWS Lambda
         * event sources.
         *
         * @param eventContextExtractorResolver the {@link LambdaEventContextExtractorResolver} instance
         *                                      to be used for extracting trace context from Lambda events
         * @return the updated Builder instance for method chaining
         */
        public Builder eventContextExtractorResolver(
                LambdaEventContextExtractorResolver eventContextExtractorResolver) {
            this.eventContextExtractorResolver = eventContextExtractorResolver;
            return this;
        }

        /**
         * Constructs a new instance of TracingOpenTelemetry using the current state of the Builder.
         * This method finalizes the configuration and returns the configured TracingOpenTelemetry instance.
         *
         * @return a fully configured TracingOpenTelemetry instance
         */
        public TracingOpenTelemetry build() {
            return new TracingOpenTelemetry(this);
        }
    }
}