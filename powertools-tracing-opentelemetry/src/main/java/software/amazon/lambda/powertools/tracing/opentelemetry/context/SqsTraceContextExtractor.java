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

        for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {

            if (message == null || message.getMessageAttributes() == null) {
                continue;
            }

            Map<String, SQSEvent.MessageAttribute> attributes = message.getMessageAttributes();

            if (attributes.isEmpty()) {
                continue;
            }

            Map<String, String> propagationAttributes = attributes.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() != null)
                    .filter(entry -> entry.getValue().getStringValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getStringValue()
                    ));

            Context extractedContext = propagator.extract(
                    parentContext,
                    propagationAttributes,
                    OpenTelemetryProvider.textMapGetter()
            );

            if (Span.fromContext(extractedContext).getSpanContext().isValid()) {

                return extractedContext;
            }
        }

        return parentContext;
    }

    @Override
    public void enrichSpan(Object event, Span span) {
        SQSEvent sqsEvent = (SQSEvent) event;

        if (sqsEvent.getRecords() == null || sqsEvent.getRecords().isEmpty()) {
            return;
        }

        span.setAttribute("messaging.system", "aws.sqs");

        span.setAttribute("messaging.batch.message_count", sqsEvent.getRecords().size());

        SQSEvent.SQSMessage message = sqsEvent.getRecords().get(0);

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