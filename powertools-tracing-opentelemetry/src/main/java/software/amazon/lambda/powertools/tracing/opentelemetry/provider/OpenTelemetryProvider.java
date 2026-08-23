/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.TraceContextPropagationMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.LambdaResource;

public final class OpenTelemetryProvider {

    private static final String INSTRUMENTATION_NAME = "aws-lambda-powertools";
    private static final String TRACE_CONTEXT_PROPAGATION_MODE_ENV = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE";
    private static final int MAX_EXPORT_BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long SCHEDULE_DELAY_MILLIS = 1_000;
    private static final long EXPORT_TIMEOUT_MILLIS = 3_000;

    private static final SdkTracerProvider TRACER_PROVIDER = createTracerProvider();
    private static final TraceContextPropagationMode TRACE_CONTEXT_PROPAGATION_MODE = retrieveTraceContextMode();
    private static final TextMapGetter<Map<String, String>> TEXT_MAP_GETTER = createTextMapGetter();
    private static final TextMapPropagator PROPAGATOR = createPropagator();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OpenTelemetryProvider() {
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    public static TraceContextPropagationMode traceContextPropagationMode() {
        return TRACE_CONTEXT_PROPAGATION_MODE;
    }

    public static Tracer tracer() {
        return TRACER_PROVIDER.get(INSTRUMENTATION_NAME);
    }

    public static SdkTracerProvider tracerProvider() {
        return TRACER_PROVIDER;
    }

    public static TextMapPropagator propagator() {
        return PROPAGATOR;
    }

    public static TextMapGetter<Map<String, String>> textMapGetter() {
        return TEXT_MAP_GETTER;
    }

    private static TraceContextPropagationMode retrieveTraceContextMode() {

        String value = SystemWrapper.getenv(TRACE_CONTEXT_PROPAGATION_MODE_ENV);

        if (value == null || value.isBlank()) {
            return TraceContextPropagationMode.PARENT;
        }

        try {
            return TraceContextPropagationMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return TraceContextPropagationMode.PARENT;
        }
    }

    private static TextMapGetter<Map<String, String>> createTextMapGetter() {
        return new TextMapGetter<>() {

            @Override
            public Iterable<String> keys(Map<String, String> carrier) {

                return carrier == null
                        ? Collections.emptyList()
                        : carrier.keySet();
            }

            @Override
            public String get(Map<String, String> carrier, String key) {

                return carrier == null
                        ? null
                        : carrier.get(key);
            }
        };
    }

    private static SdkTracerProvider createTracerProvider() {

        OtlpGrpcSpanExporter exporter =
                OtlpGrpcSpanExporter.builder()
                        .setTimeout(
                                EXPORT_TIMEOUT_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .build();

        BatchSpanProcessor processor =
                BatchSpanProcessor.builder(exporter)
                        .setMaxExportBatchSize(MAX_EXPORT_BATCH_SIZE)
                        .setMaxQueueSize(MAX_QUEUE_SIZE)
                        .setScheduleDelay(
                                SCHEDULE_DELAY_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .setExporterTimeout(
                                EXPORT_TIMEOUT_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .build();

        return SdkTracerProvider.builder()
                .setResource(LambdaResource.create())
                .addSpanProcessor(processor)
                .build();
    }
    
    private static TextMapPropagator createPropagator() {
        return TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                AwsXrayPropagator.getInstance()
        );
    }
}