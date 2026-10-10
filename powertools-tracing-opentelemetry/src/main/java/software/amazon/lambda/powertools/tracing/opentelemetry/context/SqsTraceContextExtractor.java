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

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

/**
 * SqsTraceContextExtractor is responsible for extracting and enriching trace context
 * information from AWS SQS events in the context of AWS Lambda functions. It implements
 * the {@code LambdaEventContextExtractor} interface, providing functionality to determine
 * support for an event, extract trace context, and enrich spans with additional attributes.
 * <p>
 * The class processes SQS events by iterating through the batch of SQS messages, extracting
 * propagation headers from message attributes, and building trace context information to be
 * propagated and used by OpenTelemetry.
 * <p>
 * Key functionalities include:
 * - Determining if the extractor supports the provided event.
 * - Extracting trace context from propagation headers present in SQS message attributes.
 * - Enriching spans with messaging system details, including the number of messages in a batch
 * and the queue name from the event source.
 * <p>
 * This class is intended for use with AWS Lambda functions processing SQS events for tracing
 * distributed systems.
 * <p>
 * Thread-safety: This class is immutable and thread-safe.
 */
public final class SqsTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof SQSEvent;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        SQSEvent sqsEvent = (SQSEvent) event;

        if (sqsEvent.getRecords() == null || sqsEvent.getRecords().isEmpty()) {
            return new ExtractedTraceContext(parentContext, List.of(), SpanKind.CONSUMER);
        }

        List<SpanContext> spanContexts = new ArrayList<>();

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

        return new ExtractedTraceContext(parent, spanContexts, SpanKind.CONSUMER);
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