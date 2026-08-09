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

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * A utility class that manages the lifecycle of a span and its associated context
 * within a thread. It ensures that the span is properly closed and the thread context
 * is restored when the scope is closed.
 * <p>
 * This class is primarily used to work with OpenTelemetry spans, making them current
 * in the thread context and managing their lifecycle, including recording exceptions
 * and handling automatic cleanup of associated resources.
 * <p>
 * It implements {@link AutoCloseable}, allowing it to be used in try-with-resources blocks
 * to ensure proper cleanup of the span and scope.
 */
public final class SpanScope implements AutoCloseable {

    private final Span span;
    private final Scope scope;

    public SpanScope(Span span) {
        this.span = span;
        this.scope = span.makeCurrent();
    }

    /**
     * Retrieves the {@link Span} associated with this {@link SpanScope}.
     *
     * @return the {@link Span} managed by this {@link SpanScope}
     */
    public Span span() {
        return span;
    }

    /**
     * Records an exception in the span and sets its status to {@code StatusCode.ERROR}.
     *
     * @param throwable the {@link Throwable} instance to be recorded as an event in the span.
     */
    public void recordException(Throwable throwable) {
        span.recordException(throwable);
        span.setStatus(StatusCode.ERROR);
    }

    @Override
    public void close() {
        scope.close();
        span.end();
    }
}
