package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

public final class SnsTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof SNSEvent;
    }

    @Override
    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        SNSEvent snsEvent = (SNSEvent) event;

        if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
            return parentContext;
        }

        SNSEvent.SNSRecord record = snsEvent.getRecords().get(0);

        Map<String, SNSEvent.MessageAttribute> attributes = record.getSNS().getMessageAttributes();

        if (attributes == null || attributes.isEmpty()) {
            return parentContext;
        }

        Map<String, String> propagationAttributes = attributes.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getValue()
                ));

        return propagator.extract(
                parentContext,
                propagationAttributes,
                OpenTelemetryProvider.textMapGetter()
        );
    }

    @Override
    public void enrichSpan(Object event, Span span) {

        SNSEvent snsEvent = (SNSEvent) event;

        if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
            return;
        }

        SNSEvent.SNSRecord record = snsEvent.getRecords().get(0);

        if (record.getSNS() == null) {
            return;
        }

        span.setAttribute("messaging.system", "aws.sns");

        if (record.getSNS().getMessageId() != null) {
            span.setAttribute("messaging.message.id", record.getSNS().getMessageId());
        }

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