package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;

public final class KinesisTraceContextExtractor
        implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof KinesisEvent;
    }

    @Override
    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        /*
         * Kinesis records do not expose message attributes
         * that can be used for W3C trace context propagation.
         *
         * Do not assume that traceparent is stored inside the Kinesis
         * record payload. Propagation through Kinesis should be
         * defined by a dedicated propagation strategy if supported in
         * the future.
         */

        return parentContext;
    }

    @Override
    public void enrichSpan(Object event, Span span) {

        KinesisEvent kinesisEvent = (KinesisEvent) event;

        if (kinesisEvent.getRecords() == null || kinesisEvent.getRecords().isEmpty()) {
            return;
        }

        KinesisEvent.KinesisEventRecord firstRecord = kinesisEvent.getRecords().get(0);

        if (firstRecord == null || firstRecord.getKinesis() == null) {
            return;
        }

        KinesisEvent.Record kinesis = firstRecord.getKinesis();

        span.setAttribute("messaging.system", "aws.kinesis");

        if (kinesis.getPartitionKey() != null) {
            span.setAttribute("messaging.partition_key", kinesis.getPartitionKey());
        }

        if (kinesis.getSequenceNumber() != null) {
            span.setAttribute("messaging.message.id", kinesis.getSequenceNumber());
        }

        if (kinesis.getApproximateArrivalTimestamp() != null) {
            span.setAttribute("messaging.message.receive.timestamp",
                    kinesis.getApproximateArrivalTimestamp().getTime());
        }

        if (firstRecord.getEventSourceARN() != null) {
            span.setAttribute("messaging.destination.name", extractStreamName(firstRecord.getEventSourceARN()));
        }
    }


    private String extractStreamName(String arn) {
        int separator = arn.lastIndexOf('/');

        return separator >= 0
                ? arn.substring(separator + 1)
                : arn;
    }
}
