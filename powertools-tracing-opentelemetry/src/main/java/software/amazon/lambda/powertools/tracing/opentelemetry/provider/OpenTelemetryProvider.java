/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.TraceContextPropagationMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.LambdaResource;

public final class OpenTelemetryProvider {

    private static final String INSTRUMENTATION_NAME = "aws-lambda-powertools";
    private static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL = "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL";
    private static final String TRACE_CONTEXT_PROPAGATION_MODE_ENV = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE";

    private static final int MAX_EXPORT_BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long SCHEDULE_DELAY_MILLIS = 1_000;
    private static final long EXPORT_TIMEOUT_MILLIS = 3_000;

    private static final OpenTelemetry OPEN_TELEMETRY = initializeOpenTelemetry();

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
        return OPEN_TELEMETRY.getTracer(INSTRUMENTATION_NAME);
    }

    public static TextMapPropagator propagator() {
        return PROPAGATOR;
    }

    public static TextMapGetter<Map<String, String>> textMapGetter() {
        return TEXT_MAP_GETTER;
    }

    /**
     * Uses an already configured GlobalOpenTelemetry instance when one exists.
     * <p>
     * This is important when running with the ADOT Lambda layer/javaagent,
     * because the agent configures the global OpenTelemetry instance with
     * its own TracerProvider, exporters, processors, resources, etc.
     * <p>
     * If no global OpenTelemetry instance has been configured, Powertools
     * creates its own Lambda-optimized default configuration.
     */
    private static OpenTelemetry initializeOpenTelemetry() {

        OpenTelemetry globalOpenTelemetry = GlobalOpenTelemetry.get();

        if (!isNoop(globalOpenTelemetry)) {
            return globalOpenTelemetry;
        }

        return createDefaultOpenTelemetry();
    }

    /**
     * Determines whether GlobalOpenTelemetry has been configured.
     * <p>
     * GlobalOpenTelemetry.get() returns OpenTelemetry.noop() when no
     * SDK/global implementation has been registered.
     */
    private static boolean isNoop(OpenTelemetry openTelemetry) {
        return openTelemetry == OpenTelemetry.noop();
    }

    /**
     * Creates the Powertools default OpenTelemetry configuration.
     */
    private static OpenTelemetry createDefaultOpenTelemetry() {

        return OpenTelemetrySdk.builder()
                .setTracerProvider(createTracerProvider())
                .setPropagators(ContextPropagators.create(PROPAGATOR))
                .build();
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

        String protocol = SystemWrapper.getenv(OTEL_EXPORTER_OTLP_TRACES_PROTOCOL);

        SpanExporter exporter = createExporter(protocol);

        BatchSpanProcessor processor = BatchSpanProcessor.builder(exporter)
                .setMaxExportBatchSize(MAX_EXPORT_BATCH_SIZE)
                .setMaxQueueSize(MAX_QUEUE_SIZE)
                .setScheduleDelay(SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .setExporterTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build();

        return SdkTracerProvider.builder()
                .setResource(LambdaResource.create())
                .addSpanProcessor(processor)
                .build();
    }

    private static SpanExporter createExporter(String protocol) {

        if (protocol == null || protocol.isBlank()) {
            return OtlpGrpcSpanExporter.builder()
                    .setTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .build();
        }

        switch (protocol.trim().toLowerCase()) {
            case "grpc":
                return OtlpGrpcSpanExporter.builder()
                        .setTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                        .build();

            case "http/protobuf":
                return OtlpHttpSpanExporter.builder()
                        .setTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                        .build();

            default:
                throw new IllegalArgumentException(
                        "Unsupported OTLP protocol: " + protocol
                );
        }
    }

    private static TextMapPropagator createPropagator() {

        return TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                AwsXrayPropagator.getInstance()
        );
    }
}