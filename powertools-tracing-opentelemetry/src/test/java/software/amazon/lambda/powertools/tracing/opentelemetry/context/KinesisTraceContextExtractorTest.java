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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KinesisTraceContextExtractorTest {

    @Mock
    private Span span;

    @Mock
    private TextMapPropagator propagator;

    @InjectMocks
    private KinesisTraceContextExtractor extractor;

    @Test
    void shouldSupportKinesisEvent() {
        KinesisEvent kinesisEvent = new KinesisEvent();

        boolean result = extractor.supports(kinesisEvent);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportNonKinesisEvent() {
        assertThat(extractor.supports("Some non-Kinesis event")).isFalse();
        assertThat(extractor.supports(new Object())).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    void shouldExtractTraceContextWithConsumerSpanKindAndNoSpanContexts() {
        KinesisEvent event = new KinesisEvent();
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsNull() {
        KinesisEvent event = new KinesisEvent();
        event.setRecords(null);

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsEmpty() {
        KinesisEvent event = new KinesisEvent();
        event.setRecords(Collections.emptyList());

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenFirstRecordIsNull() {
        KinesisEvent event = new KinesisEvent();
        event.setRecords(Collections.singletonList(null));

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenKinesisIsNull() {
        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(null);

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldEnrichSpanWithKinesisMetadata() {
        KinesisEvent.Record kinesis = new KinesisEvent.Record();
        kinesis.setPartitionKey("partition-1");
        kinesis.setSequenceNumber("12345678901234567890");
        kinesis.setApproximateArrivalTimestamp(new Date(1609459200000L));

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(kinesis);
        record.setEventSourceARN("arn:aws:kinesis:us-east-1:123456789012:stream/test-stream");

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.kinesis");
        verify(span).setAttribute("messaging.partition_key", "partition-1");
        verify(span).setAttribute("messaging.message.id", "12345678901234567890");
        verify(span).setAttribute("messaging.message.receive.timestamp", 1609459200000L);
        verify(span).setAttribute("messaging.destination.name", "test-stream");
    }

    @Test
    void shouldEnrichSpanWithStreamNameFromArnWithoutSlash() {
        KinesisEvent.Record kinesis = new KinesisEvent.Record();

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(kinesis);
        record.setEventSourceARN("stream-name-without-separator");

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.kinesis");
        verify(span).setAttribute("messaging.destination.name", "stream-name-without-separator");
    }

    @Test
    void shouldNotEnrichSpanWithPartitionKeyWhenNull() {
        KinesisEvent.Record kinesis = new KinesisEvent.Record();
        kinesis.setPartitionKey(null);

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(kinesis);

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.kinesis");
        verify(span, never()).setAttribute(eq("messaging.partition_key"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWithSequenceNumberWhenNull() {
        KinesisEvent.Record kinesis = new KinesisEvent.Record();
        kinesis.setSequenceNumber(null);

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(kinesis);

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.kinesis");
        verify(span, never()).setAttribute(eq("messaging.message.id"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWithTimestampWhenNull() {
        KinesisEvent.Record kinesis = new KinesisEvent.Record();
        kinesis.setApproximateArrivalTimestamp(null);

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(kinesis);

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.kinesis");
        verify(span, never()).setAttribute(eq("messaging.message.receive.timestamp"), anyLong());
    }

    @Test
    void shouldNotEnrichSpanWithDestinationNameWhenEventSourceArnIsNull() {
        KinesisEvent.Record kinesis = new KinesisEvent.Record();

        KinesisEvent.KinesisEventRecord record = new KinesisEvent.KinesisEventRecord();
        record.setKinesis(kinesis);
        record.setEventSourceARN(null);

        KinesisEvent event = new KinesisEvent();
        event.setRecords(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.kinesis");
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
    }
}