package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import java.util.List;

public final class ExtractedTraceContext {

    private final Context parentContext;
    private final List<SpanContext> spanContexts;
    private final SpanKind spanKind;

    public ExtractedTraceContext(Context parentContext, List<SpanContext> spanContexts, SpanKind spanKind) {
        this.parentContext = parentContext;
        this.spanContexts = spanContexts;
        this.spanKind = spanKind;
    }

    public ExtractedTraceContext(Context parentContext, List<SpanContext> spanContexts) {
        this.parentContext = parentContext;
        this.spanContexts = spanContexts;
        this.spanKind = SpanKind.SERVER;
    }

    public Context context() {
        return parentContext;
    }

    public List<SpanContext> spanContexts() {
        return spanContexts;
    }

    public SpanKind spanKind() {
        return spanKind;
    }
}
