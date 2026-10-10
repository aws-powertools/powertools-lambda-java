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

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import java.util.Objects;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.tracing.opentelemetry.Tracing;
import software.amazon.lambda.powertools.tracing.opentelemetry.TracingOpenTelemetry;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.ExtractedTraceContext;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.TraceContextPropagationMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.provider.OpenTelemetryProvider;

/**
 * TracingOpenTelemetryAspect is an AspectJ aspect that facilitates tracing for methods annotated
 * with the {@link Tracing} annotation. It integrates with OpenTelemetry to automatically create and
 * manage spans for annotated methods, capturing execution context, responses, and errors.
 *
 * <h2>Functional Overview:</h2>
 * - Creates and manages OpenTelemetry spans around methods annotated with {@link Tracing}.
 * - Supports both handler and internal method spans.
 * - Extracts contextual information if available to enrich spans.
 * - Captures response data and errors based on configurable capture modes.
 * - Flushes telemetry data upon span completion.
 *
 * <h2>Key Methods:</h2>
 * <ul>
 * <li>configure - Sets a custom {@link TracingOpenTelemetry} instance to be used.</li>
 * <li>callAt - Defines the pointcut for methods annotated with {@link Tracing}.</li>
 * <li>around - Core functionality that wraps the target method execution with a span.</li>
 * </ul>
 *
 * <h2>Trace Context Handling:</h2>
 * - Extracts trace context for handler methods for more seamless propagation.
 * - Supports linking spans or connecting to existing parent spans based on configuration.
 *
 * <h2>Capture Modes:</h2>
 * - The capture modes ({@link Tracing.CaptureMode}) dictate whether and how responses and errors are
 *   recorded in spans:
 *   - RESPONSE_AND_ERROR: Captures both responses and errors.
 *   - RESPONSE: Captures only responses.
 *   - ERROR: Captures only errors.
 *   - DISABLED: Disables any capture.
 *   - ENVIRONMENT_VAR: Determines capture based on environment variables.
 *
 * <h2>Span Creation:</h2>
 * - Handler spans include additional AWS Lambda-related metadata if applicable.
 * - Internal method spans are marked with a default {@link SpanKind#INTERNAL}.
 *
 * <h2>Error Handling:</h2>
 * - Ensures exceptions are propagated while recording them in the span if enabled.
 *
 * <h2>Thread Safety:</h2>
 * - The class ensures thread safety for span management in concurrent environments.
 * <p>
 * Note: This class requires OpenTelemetry to be properly configured in the application context.
 */
@Aspect
public final class TracingOpenTelemetryAspect {

    // Cannot be final for testing purposes
    private static TracingOpenTelemetry tracingOtel = TracingOpenTelemetry.create();

    public static void configure(TracingOpenTelemetry tracing) {
        tracingOtel = Objects.requireNonNull(tracing);
    }

    @SuppressWarnings("EmptyMethod")
    @Pointcut("@annotation(tracing)")
    public void callAt(Tracing tracing) {
    }

    @Around(
            value = "callAt(tracing) && execution(@Tracing * *.*(..))",
            argNames = "pjp,tracing"
    )
    public Object around(ProceedingJoinPoint pjp, Tracing tracing) throws Throwable {

        String spanName = tracing.spanName().isEmpty()
                ? pjp.getSignature().getName()
                : tracing.spanName();

        if (isHandlerMethod(pjp)) {
            return traceHandler(pjp, tracing, spanName);
        }

        return traceMethod(pjp, tracing, spanName);
    }

    private Object traceHandler(ProceedingJoinPoint pjp, Tracing tracing, String spanName) throws Throwable {

        ExtractedTraceContext extractedTraceContext = extractTraceContext(pjp);

        try (SpanScope scope = addHandlerSpan(spanName, extractedTraceContext)) {

            Span span = scope.span();

            tracingOtel.eventContextExtractorResolver().enrichSpan(pjp.getArgs()[0], span);

            addLambdaInvocationAttributes(pjp, span);

            try {

                Object result = pjp.proceed(pjp.getArgs());

                captureResponse(span, tracing, result);

                coldStartDone();

                return result;

            } catch (Throwable throwable) {

                captureError(scope, tracing, throwable);

                throw throwable;
            }
        } finally {
            tracingOtel.flush();
        }
    }

    private SpanScope addHandlerSpan(String spanName, ExtractedTraceContext extractedTraceContext) {

        if (shouldUseSpanLinks(extractedTraceContext)) {
            return tracingOtel.addSpan(
                    spanName,
                    extractedTraceContext.spanKind(),
                    handlerAttributes(),
                    Context.current(),
                    extractedTraceContext.spanContexts()
            );
        }

        return tracingOtel.addSpan(
                spanName,
                extractedTraceContext.spanKind(),
                handlerAttributes(),
                extractedTraceContext.context()
        );
    }

    private boolean shouldUseSpanLinks(ExtractedTraceContext extractedTraceContext) {

        return OpenTelemetryProvider.traceContextPropagationMode() == TraceContextPropagationMode.LINK
                && !extractedTraceContext.spanContexts().isEmpty();
    }

    private Object traceMethod(ProceedingJoinPoint pjp, Tracing tracing, String spanName) throws Throwable {

        try (SpanScope scope = tracingOtel.addSpan(spanName, SpanKind.INTERNAL, Attributes.empty(),
                Context.current())) {

            Span span = scope.span();

            try {
                Object result = pjp.proceed(pjp.getArgs());

                captureResponse(span, tracing, result);

                return result;

            } catch (Throwable throwable) {

                captureError(scope, tracing, throwable);

                throw throwable;
            }
        }
    }

    private ExtractedTraceContext extractTraceContext(ProceedingJoinPoint pjp) {

        return tracingOtel.eventContextExtractorResolver().extract(
                pjp.getArgs()[0],
                Context.current(),
                tracingOtel.propagator()
        );
    }

    private Attributes handlerAttributes() {
        return Attributes.builder()
                .put(AttributesConstants.FAAS_COLDSTART, isColdStart())
                .build();
    }

    private void addLambdaInvocationAttributes(ProceedingJoinPoint pjp, Span span) {

        Optional.ofNullable(LambdaHandlerProcessor.extractContext(pjp))
                .ifPresent(
                        context -> span.setAttribute(AttributesConstants.FAAS_INVOCATION_ID, context.getAwsRequestId()
                        )
                );
    }

    private void captureResponse(Span span, Tracing tracing, Object response) throws Exception {

        if (!isCaptureResponseEnabled(tracing)) {
            return;
        }

        span.setAttribute(
                AttributesConstants.RESPONSE_ATTRIBUTE,
                OpenTelemetryProvider.objectMapper().writeValueAsString(response)
        );
    }

    private void captureError(SpanScope scope, Tracing tracing, Throwable throwable) {

        if (isCaptureErrorEnabled(tracing)) {
            scope.recordException(throwable);
        }
    }

    private boolean isCaptureResponseEnabled(Tracing tracing) {
        switch (tracing.captureMode()) {
            case ENVIRONMENT_VAR:
                return isEnvironmentVariableSet(
                        AttributesConstants.CAPTURE_RESPONSE_ENV)
                        && environmentVariable(
                        AttributesConstants.CAPTURE_RESPONSE_ENV);

            case RESPONSE:
            case RESPONSE_AND_ERROR:
                return true;

            case DISABLED:
            case ERROR:
            default:
                return false;
        }
    }

    private boolean isCaptureErrorEnabled(Tracing tracing) {
        switch (tracing.captureMode()) {
            case ENVIRONMENT_VAR:
                return isEnvironmentVariableSet(
                        AttributesConstants.CAPTURE_ERROR_ENV)
                        && environmentVariable(
                        AttributesConstants.CAPTURE_ERROR_ENV);

            case ERROR:
            case RESPONSE_AND_ERROR:
                return true;

            case DISABLED:
            case RESPONSE:
            default:
                return false;
        }
    }

    private boolean environmentVariable(String key) {
        return Boolean.parseBoolean(SystemWrapper.getenv(key));
    }

    private boolean isEnvironmentVariableSet(String key) {
        return SystemWrapper.containsKey(key);
    }
}