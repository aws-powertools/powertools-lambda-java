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

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;
import java.util.Objects;

/**
 * A specialized implementation of {@link LambdaEventContextExtractor} for handling AWS DynamoDB Streams events.
 * This class enables extraction of tracing context, enrichment of OpenTelemetry spans, and determination of
 * compatibility with DynamoDB Streams events for distributed tracing purposes.
 * <p>
 * Instances of this class focus on the following:
 * - Verifying if an event is a DynamoDB Streams event.
 * - Extracting trace context information in scenarios where trace context propagation is applicable.
 * - Enriching OpenTelemetry spans with metadata derived from DynamoDB Streams events, such as stream names
 * and record batch sizes.
 * <p>
 * Note: Due to limitations in DynamoDB Streams metadata, W3C trace context propagation (e.g., `traceparent`)
 * is not supported by default. Future enhancements for dedicated propagation strategies may be required.
 */
public final class DynamoDbTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof DynamodbEvent;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        /*
         * DynamoDB Streams records do not expose message attributes
         * that can be used for W3C trace context propagation.
         *
         * Do not assume that traceparent is stored inside the DynamoDB
         * record payload. Propagation through DynamoDB Streams should be
         * defined by a dedicated propagation strategy if supported in
         * the future.
         */
        return new ExtractedTraceContext(parentContext, List.of(), SpanKind.CONSUMER);
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
