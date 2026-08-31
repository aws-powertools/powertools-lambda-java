package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;

class SpanScopeTest {

    @Test
    void span_returnsCurrentSpan() {

        Span mockSpan = mock(Span.class);
        SpanScope spanScope = new SpanScope(mockSpan);

        Span result = spanScope.span();

        assertEquals(mockSpan, result, "The span method should return the same Span instance.");
    }

    @Test
    void recordException_recordsThrowableAndSetsErrorStatus() {
        Span mockSpan = mock(Span.class);
        SpanScope spanScope = new SpanScope(mockSpan);
        Throwable exception = new RuntimeException("Test exception");

        spanScope.recordException(exception);

        verify(mockSpan).recordException(exception);
        verify(mockSpan).setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
    }

    @Test
    void close_closesScopeAndEndsSpan() {

        Span mockSpan = mock(Span.class);
        Scope mockScope = mock(Scope.class);
        when(mockSpan.makeCurrent()).thenReturn(mockScope);

        SpanScope spanScope = new SpanScope(mockSpan);

        spanScope.close();

        verify(mockScope).close();
        verify(mockSpan).end();
    }
}