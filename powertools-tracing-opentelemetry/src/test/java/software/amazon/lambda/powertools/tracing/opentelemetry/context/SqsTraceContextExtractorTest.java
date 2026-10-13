/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SqsTraceContextExtractorTest {

    @Mock
    private Span span;

    @Mock
    private TextMapPropagator propagator;

    @InjectMocks
    private SqsTraceContextExtractor extractor;

    @Test
    void shouldSupportSqsEvent() {
        SQSEvent sqsEvent = new SQSEvent();

        boolean result = extractor.supports(sqsEvent);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportNonSqsEvent() {
        assertThat(extractor.supports("Some non-SQS event")).isFalse();
        assertThat(extractor.supports(new Object())).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    void shouldExtractTraceContextWithConsumerSpanKindWhenRecordsIsNull() {
        SQSEvent event = new SQSEvent();
        event.setRecords(null);
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void shouldExtractTraceContextWithConsumerSpanKindWhenRecordsIsEmpty() {
        SQSEvent event = new SQSEvent();
        event.setRecords(Collections.emptyList());
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void shouldExtractTraceContextFromSqsEvent() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String spanId = "00f067aa0ba902b7";

        SQSEvent.MessageAttribute traceparent = new SQSEvent.MessageAttribute();
        traceparent.setStringValue("00-" + traceId + "-" + spanId + "-01");

        Map<String, SQSEvent.MessageAttribute> messageAttributes = new HashMap<>();
        messageAttributes.put("traceparent", traceparent);

        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageAttributes(messageAttributes);

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));

        ExtractedTraceContext extractedContext = extractor.extract(
                event,
                Context.current(),
                W3CTraceContextPropagator.getInstance()
        );

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.spanContexts()).hasSize(1);
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);

        SpanContext spanContext = extractedContext.spanContexts().get(0);
        assertThat(spanContext.isValid()).isTrue();
        assertThat(spanContext.getTraceId()).isEqualTo(traceId);
        assertThat(spanContext.getSpanId()).isEqualTo(spanId);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsNull() {
        SQSEvent event = new SQSEvent();
        event.setRecords(null);

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsEmpty() {
        SQSEvent event = new SQSEvent();
        event.setRecords(Collections.emptyList());

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldEnrichSpanWithSqsMetadata() {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:test-queue");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sqs");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span).setAttribute("messaging.destination.name", "test-queue");
    }

    @Test
    void shouldEnrichSpanWithQueueNameFromArnWithoutColon() {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setEventSourceArn("queue-name-without-separator");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sqs");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span).setAttribute("messaging.destination.name", "queue-name-without-separator");
    }

    @Test
    void shouldNotEnrichSpanWithDestinationNameWhenEventSourceArnIsNull() {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setEventSourceArn(null);

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sqs");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
    }

    @Test
    void shouldEnrichSpanWithBatchCountForMultipleMessages() {
        SQSEvent.SQSMessage message1 = new SQSEvent.SQSMessage();
        message1.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:test-queue");

        SQSEvent.SQSMessage message2 = new SQSEvent.SQSMessage();
        message2.setEventSourceArn("arn:aws:sqs:us-east-1:123456789012:test-queue");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message1, message2));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sqs");
        verify(span).setAttribute("messaging.batch.message_count", 2);
        verify(span).setAttribute("messaging.destination.name", "test-queue");
    }
}