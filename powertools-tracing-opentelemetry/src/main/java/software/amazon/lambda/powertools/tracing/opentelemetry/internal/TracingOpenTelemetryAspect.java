/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
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

@Aspect
public final class TracingOpenTelemetryAspect {

    // Cannot be final for testing purposes
    private static TracingOpenTelemetry tracingOtel = TracingOpenTelemetry.create();

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