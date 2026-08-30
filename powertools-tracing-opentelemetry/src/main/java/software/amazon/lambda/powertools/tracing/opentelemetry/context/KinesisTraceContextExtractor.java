/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;

/**
 * An implementation of the {@link LambdaEventContextExtractor} interface designed
 * for AWS Lambda functions that are triggered by Kinesis events. This class
 * provides methods for extracting trace context, determining support for
 * Kinesis events, and enriching spans with metadata specific to Kinesis.
 * <p>
 * Trace propagation for Kinesis is limited, as Kinesis records do not inherently
 * include W3C trace context propagation attributes. As such, this implementation
 * assumes trace context is not present in the payload and instead defines how
 * future propagation strategies could be supported.
 * <p>
 * This class primarily handles the following responsibilities:
 * - Identifies whether a given event is a Kinesis event.
 * - Extracts minimal trace context from a Kinesis event, returning a consumer
 * span kind without assuming additional trace attributes.
 * - Enriches spans with Kinesis-specific attributes, such as partition key,
 * sequence number, approximate arrival timestamp, and stream name.
 * <p>
 * It is intended for use in distributed tracing scenarios within AWS Lambda
 * functions, ensuring that spans generated for Kinesis events are annotated
 * with meaningful metadata.
 */
public final class KinesisTraceContextExtractor
        implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof KinesisEvent;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        /*
         * Kinesis records do not expose message attributes
         * that can be used for W3C trace context propagation.
         *
         * Do not assume that traceparent is stored inside the Kinesis
         * record payload. Propagation through Kinesis should be
         * defined by a dedicated propagation strategy if supported in
         * the future.
         */

        return new ExtractedTraceContext(parentContext, List.of(), SpanKind.CONSUMER);
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
