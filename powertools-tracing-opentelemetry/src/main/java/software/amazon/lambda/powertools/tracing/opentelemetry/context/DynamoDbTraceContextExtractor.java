package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Objects;

public final class DynamoDbTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof DynamodbEvent;
    }

    @Override
    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        /*
         * DynamoDB Streams records do not expose message attributes
         * that can be used for W3C trace context propagation.
         *
         * Do not assume that traceparent is stored inside the DynamoDB
         * record payload. Propagation through DynamoDB Streams should be
         * defined by a dedicated propagation strategy if supported in
         * the future.
         */
        return parentContext;
    }

    @Override
    public void enrichSpan(Object event, Span span) {

        DynamodbEvent dynamoDBEvent = (DynamodbEvent) event;

        if (dynamoDBEvent.getRecords() == null || dynamoDBEvent.getRecords().isEmpty()) {
            return;
        }

        DynamodbEvent.DynamodbStreamRecord record = dynamoDBEvent.getRecords()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (record == null) {
            return;
        }

        span.setAttribute("messaging.system", "aws.dynamodb");

        span.setAttribute("messaging.batch.message_count", dynamoDBEvent.getRecords().size());
        if (record.getEventSourceARN() != null) {
            span.setAttribute("messaging.destination.name", extractStreamName(record.getEventSourceARN()));
        }
    }

    private String extractStreamName(String streamArn) {
        int separator = streamArn.lastIndexOf('/');

        return separator >= 0
                ? streamArn.substring(separator + 1)
                : streamArn;
    }
}
