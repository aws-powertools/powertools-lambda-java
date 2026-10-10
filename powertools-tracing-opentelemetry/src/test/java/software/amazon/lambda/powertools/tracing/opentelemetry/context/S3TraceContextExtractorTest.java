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

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3TraceContextExtractorTest {

    @Mock
    private Span span;

    @Mock
    private TextMapPropagator propagator;

    @InjectMocks
    private S3TraceContextExtractor extractor;

    @Test
    void shouldSupportS3Event() {
        S3Event s3Event = new S3Event(Collections.emptyList());

        boolean result = extractor.supports(s3Event);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportNonS3Event() {
        assertThat(extractor.supports("Some non-S3 event")).isFalse();
        assertThat(extractor.supports(new Object())).isFalse();
        assertThat(extractor.supports(null)).isFalse();
    }

    @Test
    void shouldExtractTraceContextWithConsumerSpanKindAndNoSpanContexts() {
        S3Event event = new S3Event(Collections.emptyList());
        Context parentContext = Context.current();

        ExtractedTraceContext extractedContext = extractor.extract(event, parentContext, propagator);

        assertThat(extractedContext).isNotNull();
        assertThat(extractedContext.context()).isEqualTo(parentContext);
        assertThat(extractedContext.spanContexts()).isEmpty();
        assertThat(extractedContext.spanKind()).isEqualTo(SpanKind.CONSUMER);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsNull() {
        S3Event event = new S3Event(null);

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenRecordsIsEmpty() {
        S3Event event = new S3Event(Collections.emptyList());

        extractor.enrichSpan(event, span);

        verifyNoInteractions(span);
    }

    @Test
    void shouldNotEnrichSpanWhenAllRecordsAreNull() {
        S3Event event = new S3Event(Collections.singletonList(null));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.s3");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
        verify(span, never()).setAttribute(eq("messaging.event.type"), anyString());
    }

    @Test
    void shouldEnrichSpanWithS3Metadata() {
        S3EventNotification.S3BucketEntity bucket = new S3EventNotification.S3BucketEntity(
                "test-bucket", null, null);
        S3EventNotification.S3ObjectEntity object = new S3EventNotification.S3ObjectEntity(
                "test-key", 1024L, null, null, null);
        S3EventNotification.S3Entity s3 = new S3EventNotification.S3Entity(
                null, bucket, object, null);

        S3EventNotification.S3EventNotificationRecord record = new S3EventNotification.S3EventNotificationRecord(
                null, "ObjectCreated:Put", null, null, null, null, null, s3, null);

        S3Event event = new S3Event(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.s3");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span).setAttribute("messaging.destination.name", "test-bucket");
        verify(span).setAttribute("messaging.event.type", "ObjectCreated:Put");
    }

    @Test
    void shouldEnrichSpanWithBatchCountOnly() {
        S3EventNotification.S3EventNotificationRecord record1 = new S3EventNotification.S3EventNotificationRecord(
                null, null, null, null, null, null, null, null, null);
        S3EventNotification.S3EventNotificationRecord record2 = new S3EventNotification.S3EventNotificationRecord(
                null, null, null, null, null, null, null, null, null);

        S3Event event = new S3Event(List.of(record1, record2));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.s3");
        verify(span).setAttribute("messaging.batch.message_count", 2);
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
    }

    @Test
    void shouldNotEnrichSpanWithBucketNameWhenBucketIsNull() {
        S3EventNotification.S3Entity s3 = new S3EventNotification.S3Entity(
                null, null, null, null);

        S3EventNotification.S3EventNotificationRecord record = new S3EventNotification.S3EventNotificationRecord(
                null, "ObjectCreated:Put", null, null, null, null, null, s3, null);

        S3Event event = new S3Event(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.s3");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span, never()).setAttribute(eq("messaging.destination.name"), anyString());
        verify(span).setAttribute("messaging.event.type", "ObjectCreated:Put");
    }

    @Test
    void shouldNotEnrichSpanWithEventTypeWhenEventNameIsNull() {
        S3EventNotification.S3BucketEntity bucket = new S3EventNotification.S3BucketEntity(
                "test-bucket", null, null);
        S3EventNotification.S3Entity s3 = new S3EventNotification.S3Entity(
                null, bucket, null, null);

        S3EventNotification.S3EventNotificationRecord record = new S3EventNotification.S3EventNotificationRecord(
                null, null, null, null, null, null, null, s3, null);

        S3Event event = new S3Event(List.of(record));

        extractor.enrichSpan(event, span);

        verify(span).setAttribute("messaging.system", "aws.s3");
        verify(span).setAttribute("messaging.batch.message_count", 1);
        verify(span).setAttribute("messaging.destination.name", "test-bucket");
        verify(span, never()).setAttribute(eq("messaging.event.type"), anyString());
    }
}