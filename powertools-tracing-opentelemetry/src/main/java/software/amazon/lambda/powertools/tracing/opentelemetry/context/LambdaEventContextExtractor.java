package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;

public interface LambdaEventContextExtractor {

    boolean supports(Object event);

    void enrichSpan(Object event, Span span);

    ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator);
}