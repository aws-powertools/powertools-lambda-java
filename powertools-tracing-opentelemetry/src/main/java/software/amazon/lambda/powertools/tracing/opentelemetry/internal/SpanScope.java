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

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * A utility class that combines a {@link Span} and its associated {@link Scope},
 * managing the lifecycle of both. This class ensures that the span is properly ended and the scope is
 * closed when the {@code SpanScope} is no longer needed.
 *
 * <p>
 * The {@code SpanScope} class facilitates interaction with the {@link Span} during its lifecycle by
 * providing methods to set its status, add events, and record exceptions. Upon closing, the span is
 * finalized, and the associated scope is released.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * This class is not thread-safe and must be used only within the thread it was created.
 *
 * <h2>Usage</h2>
 * Instances of this class should be used in a try-with-resources block to ensure proper cleanup.
 *
 * <h2>Important Notes</h2>
 * - The {@link Span} should be created and managed by an OpenTelemetry tracer or similar system.
 * - Always close the {@code SpanScope} to release resources and end the span.
 */
public final class SpanScope implements AutoCloseable {

    private final Span span;
    private final Scope scope;

    public SpanScope(Span span) {
        this.span = span;
        this.scope = span.makeCurrent();
    }

    /**
     * Retrieves the {@link Span} associated with this {@code SpanScope}.
     *
     * @return the {@link Span} managed by this {@code SpanScope}.
     */
    public Span span() {
        return span;
    }

    /**
     * Updates the status of the associated span.
     *
     * @param status the {@link StatusCode} to set for the span
     * @return the current {@code SpanScope} instance for method chaining
     */
    public SpanScope setStatus(StatusCode status) {
        span.setStatus(status);
        return this;
    }

    /**
     * Adds an event to the associated {@link Span} with the specified name.
     *
     * @param name the name of the event to be added to the span
     * @return the current {@code SpanScope} instance for method chaining
     */
    public SpanScope addEvent(String name) {
        span.addEvent(name);
        return this;
    }

    /**
     * Adds an event with the specified name and attributes to the associated {@link Span}.
     *
     * @param name       the name of the event to add
     * @param attributes the attributes associated with the event
     * @return the current {@code SpanScope} instance for method chaining
     */
    public SpanScope addEvent(String name, Attributes attributes) {
        span.addEvent(name, attributes);
        return this;
    }

    /**
     * Records an exception in the associated {@link Span} and sets its status to {@code ERROR}.
     * This method is used to log and signal the occurrence of an error condition within the span.
     *
     * @param throwable the {@link Throwable} instance representing the exception to record
     */
    public void recordException(Throwable throwable) {
        span.recordException(throwable);
        span.setStatus(StatusCode.ERROR);
    }

    /**
     * Closes the underlying resources associated with this {@code SpanScope}.
     * This method ensures that the {@code scope} is closed to release any associated
     * context and marks the end of the {@code span}'s lifecycle by calling its {@code end()} method.
     * <p>
     * This method should be invoked to properly clean up resources and signal the end
     * of the tracing span when the scope is no longer needed.
     */
    @Override
    public void close() {
        scope.close();
        span.end();
    }
}