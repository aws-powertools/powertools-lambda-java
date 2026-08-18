package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

public final class SqsTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof SQSEvent;
    }

    @Override
    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        SQSEvent sqsEvent = (SQSEvent) event;

        if (sqsEvent.getRecords() == null || sqsEvent.getRecords().isEmpty()) {
            return parentContext;
        }

        SQSEvent.SQSMessage message = sqsEvent.getRecords().get(0);

        Map<String, SQSEvent.MessageAttribute> attributes = message.getMessageAttributes();

        if (attributes == null || attributes.isEmpty()) {
            return parentContext;
        }

        Map<String, String> propagationAttributes = attributes.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getStringValue()
                ));

        return propagator.extract(
                parentContext,
                propagationAttributes,
                OpenTelemetryProvider.textMapGetter()
        );
    }

    @Override
    public void enrichSpan(Object event, Span span) {
        SQSEvent sqsEvent = (SQSEvent) event;

        if (sqsEvent.getRecords() == null || sqsEvent.getRecords().isEmpty()) {
            return;
        }

        SQSEvent.SQSMessage message = sqsEvent.getRecords().get(0);

        span.setAttribute("messaging.system", "aws.sqs");

        if (message.getMessageId() != null) {
            span.setAttribute("messaging.message.id", message.getMessageId());
        }

        if (message.getEventSourceArn() != null) {
            span.setAttribute("messaging.destination.name", extractQueueName(message.getEventSourceArn()));
        }
    }

    private String extractQueueName(String arn) {
        int separator = arn.lastIndexOf(':');

        return separator >= 0
                ? arn.substring(separator + 1)
                : arn;
    }
}