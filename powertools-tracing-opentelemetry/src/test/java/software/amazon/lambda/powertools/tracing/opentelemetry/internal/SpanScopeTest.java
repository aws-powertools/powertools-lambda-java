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

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpanScopeTest {

    @Mock
    private Span mockSpan;

    @Mock
    private Scope mockScope;

    @BeforeEach
    void setUp() {
        when(mockSpan.makeCurrent()).thenReturn(mockScope);
    }

    @Test
    void span_shouldReturnCurrentSpan() {

        SpanScope spanScope = new SpanScope(mockSpan);

        Span result = spanScope.span();

        assertThat(result).isEqualTo(mockSpan);
    }

    @Test
    void constructor_shouldMakeSpanCurrent() {
        new SpanScope(mockSpan);

        verify(mockSpan).makeCurrent();
    }

    @Test
    void setStatus_shouldSetSpanStatus() {
        SpanScope spanScope = new SpanScope(mockSpan);
        StatusCode status = StatusCode.OK;

        SpanScope result = spanScope.setStatus(status);

        verify(mockSpan).setStatus(status);
        assertThat(result).isSameAs(spanScope);
    }

    @Test
    void setStatus_shouldAllowMethodChaining() {
        SpanScope spanScope = new SpanScope(mockSpan);

        SpanScope result = spanScope
                .setStatus(StatusCode.OK)
                .setStatus(StatusCode.ERROR);

        assertThat(result).isSameAs(spanScope);
        verify(mockSpan).setStatus(StatusCode.OK);
        verify(mockSpan).setStatus(StatusCode.ERROR);
    }

    @Test
    void addEvent_shouldAddEventWithName() {
        SpanScope spanScope = new SpanScope(mockSpan);
        String eventName = "test-event";

        SpanScope result = spanScope.addEvent(eventName);

        verify(mockSpan).addEvent(eventName);
        assertThat(result).isSameAs(spanScope);
    }

    @Test
    void addEvent_shouldAddEventWithNameAndAttributes() {

        SpanScope spanScope = new SpanScope(mockSpan);
        String eventName = "test-event";
        Attributes attributes = Attributes.builder()
                .put("key1", "value1")
                .put("key2", 42L)
                .build();

        SpanScope result = spanScope.addEvent(eventName, attributes);

        verify(mockSpan).addEvent(eventName, attributes);
        assertThat(result).isSameAs(spanScope);
    }

    @Test
    void addEvent_shouldAllowMethodChaining() {

        SpanScope spanScope = new SpanScope(mockSpan);
        Attributes attrs = Attributes.empty();

        SpanScope result = spanScope
                .addEvent("event1")
                .addEvent("event2", attrs);

        assertThat(result).isSameAs(spanScope);
        verify(mockSpan).addEvent("event1");
        verify(mockSpan).addEvent("event2", attrs);
    }

    @Test
    void recordException_shouldRecordExceptionAndSetErrorStatus() {

        SpanScope spanScope = new SpanScope(mockSpan);
        Throwable exception = new RuntimeException("Test exception");

        spanScope.recordException(exception);

        verify(mockSpan).recordException(exception);
        verify(mockSpan).setStatus(StatusCode.ERROR);
    }

    @Test
    void recordException_shouldHandleDifferentExceptionTypes() {

        SpanScope spanScope = new SpanScope(mockSpan);
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        spanScope.recordException(exception);

        verify(mockSpan).recordException(exception);
        verify(mockSpan).setStatus(StatusCode.ERROR);
    }

    @Test
    void close_shouldCloseScopeAndEndSpan() {

        SpanScope spanScope = new SpanScope(mockSpan);

        spanScope.close();

        verify(mockScope).close();
        verify(mockSpan).end();
    }

    @Test
    void spanScope_shouldWorkWithTryWithResources() {

        try (SpanScope spanScope = new SpanScope(mockSpan)) {
            spanScope.addEvent("test-event");
        }

        verify(mockSpan).addEvent("test-event");
        verify(mockScope).close();
        verify(mockSpan).end();
    }

    @Test
    void spanScope_shouldSupportFluentAPI() {

        SpanScope spanScope = new SpanScope(mockSpan);
        Attributes attrs = Attributes.builder()
                .put("error.type", "validation")
                .build();

        spanScope
                .addEvent("validation-started")
                .setStatus(StatusCode.ERROR)
                .addEvent("validation-failed", attrs);

        verify(mockSpan).addEvent("validation-started");
        verify(mockSpan).setStatus(StatusCode.ERROR);
        verify(mockSpan).addEvent("validation-failed", attrs);
    }

    @Test
    void spanScope_shouldAllowExceptionRecordingInTryWithResources() {

        RuntimeException exception = new RuntimeException("Test failure");

        try (SpanScope spanScope = new SpanScope(mockSpan)) {
            spanScope.recordException(exception);
        }

        verify(mockSpan).recordException(exception);
        verify(mockSpan).setStatus(StatusCode.ERROR);
        verify(mockScope).close();
        verify(mockSpan).end();
    }
}