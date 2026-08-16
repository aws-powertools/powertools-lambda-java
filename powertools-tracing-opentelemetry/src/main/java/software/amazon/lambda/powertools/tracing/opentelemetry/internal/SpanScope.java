package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import io.opentelemetry.api.common.Attributes;
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

    public Span span() {
        return span;
    }

    public SpanScope setStatus(StatusCode status) {
        span.setStatus(status);
        return this;
    }

    public SpanScope addEvent(String name) {
        span.addEvent(name);
        return this;
    }

    public SpanScope addEvent(String name, Attributes attributes) {
        span.addEvent(name, attributes);
        return this;
    }

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