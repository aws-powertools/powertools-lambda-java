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

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;
import java.util.Objects;

/**
 * A context extractor implementation for handling AWS S3 event notifications within an AWS Lambda environment.
 * This extractor is responsible for determining if an event can be processed, extracting trace context
 * information, and enriching spans with metadata related to the S3 event.
 * <p>
 * This implementation assumes that S3 event payloads do not contain trace context attributes (e.g.,
 * traceparent or tracestate) and handles them accordingly.
 */
public final class S3TraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof S3Event;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        /*
         * S3 event notifications do not expose message attributes
         * equivalent to SQS/SNS that can be passed directly to a
         * TextMapPropagator.
         *
         * Do not assume that traceparent/tracestate are embedded
         * inside the S3 event payload.
         */
        return new ExtractedTraceContext(parentContext, List.of(), SpanKind.CONSUMER);
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
