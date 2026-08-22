package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.util.List;

public final class ExtractedTraceContext {

    private final Context parentContext;
    private final List<SpanContext> spanContexts;

    public ExtractedTraceContext(Context parentContext, List<SpanContext> spanContexts) {
        this.parentContext = parentContext;
        this.spanContexts = spanContexts;
    }

    public Context context() {
        return parentContext;
    }

    public List<SpanContext> spanContexts() {
        return spanContexts;
    }
}
