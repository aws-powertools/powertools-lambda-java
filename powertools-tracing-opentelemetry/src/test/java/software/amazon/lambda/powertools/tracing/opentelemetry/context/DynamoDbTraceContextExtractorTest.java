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

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamoDbTraceContextExtractorTest {

    @Mock
    private Span span;

    @Mock
    private TextMapPropagator propagator;

    @InjectMocks
    private DynamoDbTraceContextExtractor extractor;

    @Test
    void shouldSupportDynamodbEvent() {
        DynamodbEvent dynamodbEvent = new DynamodbEvent();

        boolean result = extractor.supports(dynamodbEvent);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportNonDynamodbEvent() {
        assertThat(extractor.supports("Some non-Dynamodb event")).isFalse();
        assertThat(extractor.supports(new Object())).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    void shouldExtractTraceContextWithConsumerSpanKindAndNoSpanContexts() {
        DynamodbEvent event = new DynamodbEvent();
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsNull() {
        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(null);

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsEmpty() {
        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(Collections.emptyList());

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenAllRecordsAreNull() {
        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(Collections.singletonList(null));

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldEnrichSpanWithDynamoDbMetadataAndStreamNameFromArn() {
        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setEventSourceARN(
                "arn:aws:dynamodb:us-east-1:123456789012:table/TestTable/stream/2024-01-01T00:00:00.000");

        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.dynamodb");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span).setAttribute("messaging.destination.name", "2024-01-01T00:00:00.000");
    }

    @Test
    void shouldEnrichSpanWhenArnHasNoSlashSeparator() {
        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setEventSourceARN("stream-name-without-separator");

        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.dynamodb");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span).setAttribute("messaging.destination.name", "stream-name-without-separator");
    }

    @Test
    void shouldEnrichSpanWithoutDestinationNameWhenEventSourceArnIsNull() {
        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setEventSourceARN(null);

        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.dynamodb");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
    }

    @Test
    void shouldEnrichSpanWithBatchCountAndFirstNonNullRecordMetadata() {
        DynamodbEvent.DynamodbStreamRecord firstRecord = null;

        DynamodbEvent.DynamodbStreamRecord secondRecord = new DynamodbEvent.DynamodbStreamRecord();
        secondRecord.setEventSourceARN(
                "arn:aws:dynamodb:eu-west-1:123456789012:table/Orders/stream/orders-stream-2024");

        DynamodbEvent.DynamodbStreamRecord thirdRecord = new DynamodbEvent.DynamodbStreamRecord();
        thirdRecord.setEventSourceARN("arn:aws:dynamodb:eu-west-1:123456789012:table/Orders/stream/another-stream");

        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(Arrays.asList(firstRecord, secondRecord, thirdRecord));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.dynamodb");
        verify(span).setAttribute("messaging.batch.message_count", 3);
        verify(span).setAttribute("messaging.destination.name", "orders-stream-2024");
    }
}