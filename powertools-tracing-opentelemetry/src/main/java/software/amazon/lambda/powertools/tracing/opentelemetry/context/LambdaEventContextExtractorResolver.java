package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;

public final class LambdaEventContextExtractorResolver {

    private final List<LambdaEventContextExtractor> extractors;

    public LambdaEventContextExtractorResolver(List<LambdaEventContextExtractor> extractors) {

        this.extractors = List.copyOf(extractors);
    }

    public static LambdaEventContextExtractorResolver create() {
        return new LambdaEventContextExtractorResolver(
                List.of(
                        new ApiGatewayTraceContextExtractor(),
                        new SqsTraceContextExtractor(),
                        new SnsTraceContextExtractor()
                )
        );
    }

    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        return extractors.stream()
                .filter(extractor -> extractor.supports(event))
                .findFirst()
                .map(extractor ->
                        extractor.extract(
                                event,
                                parentContext,
                                propagator))
                .orElse(parentContext);
    }

    public void enrichSpan(Object event, Span span) {
        extractors.stream()
                .filter(extractor -> extractor.supports(event))
                .findFirst()
                .ifPresent(extractor -> extractor.enrichSpan(event, span));
    }
}
