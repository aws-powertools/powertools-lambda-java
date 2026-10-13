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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.tracing.opentelemetry.CaptureMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.Tracing;
import software.amazon.lambda.powertools.tracing.opentelemetry.TracingOpenTelemetry;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.ExtractedTraceContext;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.LambdaEventContextExtractorResolver;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.TraceContextPropagationMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

@ExtendWith(MockitoExtension.class)
class TracingOpenTelemetryAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private Tracing tracing;

    @Mock
    private TracingOpenTelemetry tracingOpenTelemetry;

    @Mock
    private SpanScope spanScope;

    @Mock
    private Span span;

    @Mock
    private Signature signature;

    @Mock
    private Context lambdaContext;

    @Mock
    private LambdaEventContextExtractorResolver extractorResolver;

    @Mock
    private TextMapPropagator propagator;

    private TracingOpenTelemetryAspect aspect;
    private TracingOpenTelemetry originalTracing;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        aspect = new TracingOpenTelemetryAspect();

        originalTracing = (TracingOpenTelemetry) FieldUtils
                .readStaticField(TracingOpenTelemetryAspect.class, "tracingOtel", true);

        FieldUtils.writeStaticField(TracingOpenTelemetryAspect.class, "tracingOtel", tracingOpenTelemetry, true);

        lenient().when(tracingOpenTelemetry.eventContextExtractorResolver()).thenReturn(extractorResolver);
        lenient().when(tracingOpenTelemetry.propagator()).thenReturn(propagator);
        lenient().when(spanScope.span()).thenReturn(span);
    }

    @AfterEach
    void tearDown() throws IllegalAccessException {
        FieldUtils.writeStaticField(TracingOpenTelemetryAspect.class, "tracingOtel", originalTracing, true);
    }

    @Test
    void shouldTraceHandlerMethodSuccessfully() throws Throwable {

        setupHandlerMethod();
        Object event = new Object();
        Object[] args = {event, lambdaContext};
        Object expectedResult = "result";

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.DISABLED);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn(expectedResult);

        Object result = aspect.around(pjp, tracing);

        assertThat(result).isEqualTo(expectedResult);
        verify(tracingOpenTelemetry).addSpan(eq("testHandler"), eq(SpanKind.SERVER), any(Attributes.class),
                any(io.opentelemetry.context.Context.class));
        verify(extractorResolver).enrichSpan(event, span);
        verify(tracingOpenTelemetry).flush();
    }

    @Test
    void shouldUseMethodNameWhenSpanNameIsEmpty() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};
        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("");
        when(tracing.captureMode()).thenReturn(CaptureMode.DISABLED);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn("result");

        aspect.around(pjp, tracing);

        verify(tracingOpenTelemetry).addSpan(eq("handleRequest"), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class));
    }

    @Test
    void shouldCaptureExceptionInHandler() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};
        RuntimeException expectedException = new RuntimeException("Test exception");

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.ERROR);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenThrow(expectedException);

        assertThatThrownBy(() -> aspect.around(pjp, tracing))
                .isEqualTo(expectedException);

        verify(spanScope).recordException(expectedException);
        verify(tracingOpenTelemetry).flush();
    }

    @Test
    void shouldCaptureResponseWhenModeIsResponse() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};
        String expectedResult = "test-response";

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.RESPONSE);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn(expectedResult);

        aspect.around(pjp, tracing);

        verify(span).setAttribute(eq(AttributesConstants.RESPONSE_ATTRIBUTE), anyString());
    }

    @Test
    void shouldCaptureResponseAndErrorWhenModeIsResponseAndError() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};
        RuntimeException exception = new RuntimeException("error");

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.RESPONSE_AND_ERROR);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenThrow(exception);

        assertThatThrownBy(() -> aspect.around(pjp, tracing))
                .isEqualTo(exception);

        verify(spanScope).recordException(exception);
    }

    @Test
    void shouldNotCaptureWhenModeIsDisabled() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};
        RuntimeException exception = new RuntimeException("error");

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.DISABLED);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenThrow(exception);

        assertThatThrownBy(() -> aspect.around(pjp, tracing))
                .isEqualTo(exception);

        verify(spanScope, never()).recordException(any());
        verify(span, never()).setAttribute(eq(AttributesConstants.RESPONSE_ATTRIBUTE), anyString());
    }

    @Test
    void shouldNotCaptureResponseWhenModeIsError() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.ERROR);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn("result");

        aspect.around(pjp, tracing);

        verify(span, never()).setAttribute(eq(AttributesConstants.RESPONSE_ATTRIBUTE), anyString());
    }

    @Test
    void shouldTraceNonHandlerMethodSuccessfully() throws Throwable {

        setupNonHandlerMethod();
        Object[] args = {"arg1", "arg2"};
        Object expectedResult = "result";

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testMethod");
        when(tracing.captureMode()).thenReturn(CaptureMode.DISABLED);
        when(tracingOpenTelemetry.addSpan(anyString(), eq(SpanKind.INTERNAL), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn(expectedResult);

        Object result = aspect.around(pjp, tracing);

        assertThat(result).isEqualTo(expectedResult);
        verify(tracingOpenTelemetry).addSpan(eq("testMethod"), eq(SpanKind.INTERNAL), any(Attributes.class),
                any(io.opentelemetry.context.Context.class));
        verify(extractorResolver, never()).enrichSpan(any(), any());
        verify(tracingOpenTelemetry, never()).flush();
    }

    @Test
    void shouldCaptureExceptionInNonHandlerMethod() throws Throwable {

        setupNonHandlerMethod();
        Object[] args = {"arg1"};
        RuntimeException expectedException = new RuntimeException("Method exception");

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testMethod");
        when(tracing.captureMode()).thenReturn(CaptureMode.ERROR);
        when(tracingOpenTelemetry.addSpan(anyString(), eq(SpanKind.INTERNAL), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenThrow(expectedException);

        assertThatThrownBy(() -> aspect.around(pjp, tracing))
                .isEqualTo(expectedException);

        verify(spanScope).recordException(expectedException);
    }

    @Test
    void shouldCaptureResponseInNonHandlerMethod() throws Throwable {

        setupNonHandlerMethod();
        Object[] args = {};
        String expectedResult = "method-result";

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testMethod");
        when(tracing.captureMode()).thenReturn(CaptureMode.RESPONSE);
        when(tracingOpenTelemetry.addSpan(anyString(), eq(SpanKind.INTERNAL), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn(expectedResult);

        aspect.around(pjp, tracing);

        verify(span).setAttribute(eq(AttributesConstants.RESPONSE_ATTRIBUTE), anyString());
    }

    @Test
    void shouldNotCaptureErrorInNonHandlerMethodWhenModeIsResponse() throws Throwable {

        setupNonHandlerMethod();
        Object[] args = {};
        RuntimeException exception = new RuntimeException("error");

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testMethod");
        when(tracing.captureMode()).thenReturn(CaptureMode.RESPONSE);
        when(tracingOpenTelemetry.addSpan(anyString(), eq(SpanKind.INTERNAL), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenThrow(exception);

        assertThatThrownBy(() -> aspect.around(pjp, tracing))
                .isEqualTo(exception);

        verify(spanScope, never()).recordException(any());
    }


    @Test
    void shouldConfigureCustomTracingOpenTelemetry() throws IllegalAccessException {

        TracingOpenTelemetry customTracing = mock(TracingOpenTelemetry.class);

        TracingOpenTelemetryAspect.configure(customTracing);

        TracingOpenTelemetry configured = (TracingOpenTelemetry) FieldUtils
                .readStaticField(TracingOpenTelemetryAspect.class, "tracingOtel", true);
        assertThat(configured).isEqualTo(customTracing);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConfiguringNull() {
        assertThatThrownBy(() -> TracingOpenTelemetryAspect.configure(null))
                .isInstanceOf(NullPointerException.class);
    }


    @Test
    void shouldUseSpanLinksWhenPropagationModeIsLink() throws Throwable {

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(RequestHandler.class);

        Object[] args = {new Object(), lambdaContext};
        SpanContext spanContext = mock(SpanContext.class);
        List<SpanContext> spanContexts = List.of(spanContext);

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.DISABLED);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                spanContexts,
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);

        try (MockedStatic<OpenTelemetryProvider> mockedProvider = mockStatic(OpenTelemetryProvider.class)) {
            mockedProvider.when(OpenTelemetryProvider::traceContextPropagationMode)
                    .thenReturn(TraceContextPropagationMode.LINK);

            when(tracingOpenTelemetry.addSpan(
                    anyString(),
                    any(SpanKind.class),
                    any(Attributes.class),
                    any(io.opentelemetry.context.Context.class),
                    any(List.class)))
                    .thenReturn(spanScope);

            when(pjp.proceed(args)).thenReturn("result");

            aspect.around(pjp, tracing);

            verify(tracingOpenTelemetry, times(1)).addSpan(
                    eq("testHandler"),
                    eq(SpanKind.SERVER),
                    any(Attributes.class),
                    any(io.opentelemetry.context.Context.class),
                    eq(spanContexts)
            );
        }
    }

    @Test
    void shouldUseParentContextWhenSpanContextsAreEmpty() throws Throwable {

        setupHandlerMethod();
        Object[] args = {new Object(), lambdaContext};

        when(pjp.getArgs()).thenReturn(args);
        when(tracing.spanName()).thenReturn("testHandler");
        when(tracing.captureMode()).thenReturn(CaptureMode.DISABLED);

        ExtractedTraceContext extractedContext = new ExtractedTraceContext(
                io.opentelemetry.context.Context.current(),
                Collections.emptyList(),
                SpanKind.SERVER
        );

        when(extractorResolver.extract(any(), any(), any())).thenReturn(extractedContext);
        when(tracingOpenTelemetry.addSpan(anyString(), any(SpanKind.class), any(Attributes.class),
                any(io.opentelemetry.context.Context.class)))
                .thenReturn(spanScope);
        when(pjp.proceed(args)).thenReturn("result");

        aspect.around(pjp, tracing);

        verify(tracingOpenTelemetry).addSpan(
                eq("testHandler"),
                eq(SpanKind.SERVER),
                any(Attributes.class),
                any(io.opentelemetry.context.Context.class)
        );
        verify(tracingOpenTelemetry, never()).addSpan(
                anyString(),
                any(SpanKind.class),
                any(Attributes.class),
                any(io.opentelemetry.context.Context.class),
                any(List.class)
        );
    }

    private void setupHandlerMethod() {
        lenient().when(pjp.getSignature()).thenReturn(signature);
        lenient().when(signature.getDeclaringType()).thenReturn(RequestHandler.class);
        lenient().when(signature.getName()).thenReturn("handleRequest");
    }

    private void setupNonHandlerMethod() {
        lenient().when(pjp.getSignature()).thenReturn(signature);
        lenient().when(signature.getDeclaringType()).thenReturn(TracingOpenTelemetryAspectTest.class);
        lenient().when(signature.getName()).thenReturn("someMethod");
    }
}