package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

public final class SnsTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof SNSEvent;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        SNSEvent snsEvent = (SNSEvent) event;

        if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
            return new ExtractedTraceContext(parentContext, List.of());
        }

        List<SpanContext> spanContexts = new ArrayList<>();

        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {

            if (record == null || record.getSNS() == null) {
                continue;
            }

            Map<String, SNSEvent.MessageAttribute> attributes = record.getSNS().getMessageAttributes();

            if (attributes == null || attributes.isEmpty()) {
                continue;
            }

            Map<String, String> propagationAttributes = attributes.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() != null)
                    .filter(entry -> entry.getValue().getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getValue()
                    ));

            if (propagationAttributes.isEmpty()) {
                continue;
            }

            Context extractedContext = propagator.extract(
                    Context.root(),
                    propagationAttributes,
                    OpenTelemetryProvider.textMapGetter()
            );

            SpanContext spanContext = Span.fromContext(extractedContext).getSpanContext();

            if (spanContext.isValid()) {
                spanContexts.add(spanContext);
            }
        }

        Context parent = spanContexts.isEmpty()
                ? parentContext
                : Context.root().with(Span.wrap(spanContexts.get(0)));

        return new ExtractedTraceContext(parent, spanContexts);
    }

    @Override
    public void enrichSpan(Object event, Span span) {

        SNSEvent snsEvent = (SNSEvent) event;

        if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
            return;
        }

        SNSEvent.SNSRecord record = snsEvent.getRecords()
                .stream()
                .filter(r -> r != null && r.getSNS() != null)
                .findFirst()
                .orElse(null);

        if (record == null) {
            return;
        }

        span.setAttribute("messaging.system", "aws.sns");

        if (record.getSNS().getTopicArn() != null) {
            span.setAttribute("messaging.destination.name", extractTopicName(record.getSNS().getTopicArn()));
        }
    }

    private String extractTopicName(String topicArn) {
        int separator = topicArn.lastIndexOf(':');

        return separator >= 0
                ? topicArn.substring(separator + 1)
                : topicArn;
    }
}