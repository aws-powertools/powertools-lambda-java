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

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
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
 * An implementation of {@link LambdaEventContextExtractor} specifically designed to handle AWS Simple Notification
 * Service (SNS) events.
 * This class provides mechanisms to extract trace context from SNS event records and enrich spans with relevant
 * messaging attributes.
 * It supports processing instances of {@code SNSEvent}.
 *
 * <ul>
 *   <li>{@code supports}: Determines if the given event is an instance of SNS event.</li>
 *   <li>{@code extract}: Extracts trace context data from message attributes of the SNS event records and generates
 *   an {@link ExtractedTraceContext}.</li>
 *   <li>{@code enrichSpan}: Enriches the span with attributes pertaining to the SNS messaging system, such as the
 *   topic name and messaging system specific values.</li>
 * </ul>
 * <p>
 * This class also ensures trace propagation by parsing SNS message attributes and converting them into OpenTelemetry
 * context.
 * It supports multi-record SNS events and handles cases where certain records or attributes are invalid.
 */
public final class SnsTraceContextExtractor implements LambdaEventContextExtractor {

    @Override
    public boolean supports(Object event) {
        return event instanceof SNSEvent;
    }

    @Override
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        SNSEvent snsEvent = (SNSEvent) event;

        if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
            return new ExtractedTraceContext(parentContext, List.of(), SpanKind.CONSUMER);
        }

        List<SpanContext> spanContexts = new ArrayList<>();

        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {

            if (record == null || record.getSNS() == null) {
                continue;
            }

            Map<String, SNSEvent.MessageAttribute> attributes = record.getSNS().getMessageAttributes();

            if (attributes == null || attributes.isEmpty()) {
                continue;
            }

            Map<String, String> propagationAttributes = attributes.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() != null)
                    .filter(entry -> entry.getValue().getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getValue()
                    ));

            if (propagationAttributes.isEmpty()) {
                continue;
            }

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

        SNSEvent snsEvent = (SNSEvent) event;

        if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
            return;
        }

        SNSEvent.SNSRecord record = snsEvent.getRecords()
                .stream()
                .filter(r -> r != null && r.getSNS() != null)
                .findFirst()
                .orElse(null);

        if (record == null) {
            return;
        }

        span.setAttribute("messaging.system", "aws.sns");

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