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

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
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
class SnsTraceContextExtractorTest {

    @Mock
    private Span span;

    @Mock
    private TextMapPropagator propagator;

    @InjectMocks
    private SnsTraceContextExtractor extractor;

    @Test
    void shouldSupportSnsEvent() {
        SNSEvent snsEvent = new SNSEvent();

        boolean result = extractor.supports(snsEvent);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportNonSnsEvent() {
        assertThat(extractor.supports("Some non-SNS event")).isFalse();
        assertThat(extractor.supports(new Object())).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    void shouldExtractTraceContextWithConsumerSpanKindWhenRecordsIsNull() {
        SNSEvent event = new SNSEvent();
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
        SNSEvent event = new SNSEvent();
        event.setRecords(Collections.emptyList());
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void shouldExtractTraceContextFromSnsEvent() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String spanId = "00f067aa0ba902b7";

        SNSEvent.MessageAttribute traceparent = new SNSEvent.MessageAttribute();
        traceparent.setType("String");
        traceparent.setValue("00-" + traceId + "-" + spanId + "-01");

        Map<String, SNSEvent.MessageAttribute> messageAttributes = new HashMap<>();
        messageAttributes.put("traceparent", traceparent);

        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessageAttributes(messageAttributes);

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

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
        SNSEvent event = new SNSEvent();
        event.setRecords(null);

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsEmpty() {
        SNSEvent event = new SNSEvent();
        event.setRecords(Collections.emptyList());

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenAllRecordsAreNull() {
        SNSEvent event = new SNSEvent();
        event.setRecords(Collections.singletonList(null));

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenSnsIsNull() {
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(null);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldEnrichSpanWithSnsMetadata() {
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setTopicArn("arn:aws:sns:us-east-1:123456789012:test-topic");

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sns");
        verify(span).setAttribute("messaging.destination.name", "test-topic");
    }

    @Test
    void shouldEnrichSpanWithTopicNameFromArnWithoutColon() {
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setTopicArn("topic-name-without-separator");

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sns");
        verify(span).setAttribute("messaging.destination.name", "topic-name-without-separator");
    }

    @Test
    void shouldNotEnrichSpanWithDestinationNameWhenTopicArnIsNull() {
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setTopicArn(null);

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.sns");
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
    }
}