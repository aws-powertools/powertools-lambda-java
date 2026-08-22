package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Objects;

public final class S3TraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof S3Event;
    }

    @Override
    public Context extract(Object event, Context parentContext, TextMapPropagator propagator) {

        /*
         * S3 event notifications do not expose message attributes
         * equivalent to SQS/SNS that can be passed directly to a
         * TextMapPropagator.
         *
         * Do not assume that traceparent/tracestate are embedded
         * inside the S3 event payload.
         */
        return parentContext;
    }

    @Override
    public void enrichSpan(Object event, Span span) {

        S3Event s3Event = (S3Event) event;

        if (s3Event.getRecords() == null || s3Event.getRecords().isEmpty()) {
            return;
        }

        span.setAttribute("messaging.system", "aws.s3");

        span.setAttribute("messaging.batch.message_count", s3Event.getRecords().size());

        S3EventNotification.S3EventNotificationRecord record =
                s3Event.getRecords()
                        .stream()
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

        if (record == null || record.getS3() == null) {
            return;
        }

        if (record.getS3().getBucket() != null
                && record.getS3().getBucket().getName() != null) {

            span.setAttribute("messaging.destination.name", record.getS3().getBucket().getName());
        }

        if (record.getEventName() != null) {
            span.setAttribute("messaging.event.type", record.getEventName());
        }
    }
}
