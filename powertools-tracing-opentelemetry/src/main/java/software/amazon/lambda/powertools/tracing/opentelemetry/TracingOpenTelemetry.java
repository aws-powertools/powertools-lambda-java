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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.util.Objects;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanOperation;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.SpanScope;

/**
 * A wrapper for OpenTelemetry's Tracer that simplifies the creation and management of spans.
 * This class provides utility functions for starting and controlling spans and their contexts
 * in the current execution thread.
 * <p>
 * This is a final class and cannot be extended.
 */
public final class TracingOpenTelemetry {

    private final Tracer tracer;

    private TracingOpenTelemetry(Builder builder) {
        this.tracer = builder.tracer;
    }

    /**
     * Creates a new span with the specified name and makes it the current span in the thread context.
     * The span must be manually closed to properly end it and revert the thread context.
     *
     * @param name the name of the span to be created
     * @return an instance of {@link SpanScope}, which represents the created span and its associated context
     */
    public SpanScope addSpan(String name) {
        return new SpanScope(tracer.spanBuilder(name).startSpan());
    }

    /**
     * Retrieves the current active span in the execution context.
     *
     * @return the current {@link Span} if one is active, or a default no-op {@link Span} if none is active
     */
    public Span currentSpan() {
        return Span.current();
    }

    /**
     * Executes the specified operation within the context of a new span.
     * The span is automatically managed and closed when the operation completes
     * or an exception is thrown.
     *
     * @param name the name of the span to be created
     * @param operation the operation to be executed within the span's context
     * @throws Exception if the provided operation throws an exception during execution
     */
    public void withSpan(String name, SpanOperation operation) throws Exception {
        try (SpanScope scope = addSpan(name)) {
            try {
                operation.execute(scope.span());
            } catch (Exception e) {
                scope.recordException(e);
                throw e;
            }
        }
    }

    /**
     * Creates and returns a new instance of the {@code Builder} class for constructing
     * instances of {@code TracingOpenTelemetry}.
     *
     * @return a new {@code Builder} instance for configuring and building a {@code TracingOpenTelemetry} object
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Tracer tracer;

        public Builder tracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        /**
         * Builds and returns a {@code TracingOpenTelemetry} instance configured with the specified {@code Tracer}.
         * The returned instance provides utilities for creating and managing spans.
         *
         * @return a fully constructed {@code TracingOpenTelemetry} object based on the builder's configuration
         * @throws NullPointerException if the {@code tracer} has not been set
         */
        public TracingOpenTelemetry build() {
            Objects.requireNonNull(tracer, "tracer must not be null");
            return new TracingOpenTelemetry(this);
        }
    }

}