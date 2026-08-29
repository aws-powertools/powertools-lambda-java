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
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;
import software.amazon.lambda.powertools.tracing.opentelemetry.context.TraceContextPropagationMode;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.LambdaResource;

public final class OpenTelemetryProvider {

    private static final String INSTRUMENTATION_NAME = "aws-lambda-powertools";
    private static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL = "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL";
    private static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT";
    private static final String OTEL_EXPORTER_OTLP_TRACES_HEADERS = "OTEL_EXPORTER_OTLP_TRACES_HEADERS";
    private static final String TRACE_CONTEXT_PROPAGATION_MODE_ENV = "POWERTOOLS_TRACE_CONTEXT_PROPAGATION_MODE";

    private static final int MAX_EXPORT_BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long SCHEDULE_DELAY_MILLIS = 1_000;
    private static final long EXPORT_TIMEOUT_MILLIS = 3_000;

    private static final TextMapPropagator PROPAGATOR = createPropagator();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TextMapGetter<Map<String, String>> TEXT_MAP_GETTER = createTextMapGetter();

    private static final TraceContextPropagationMode TRACE_CONTEXT_PROPAGATION_MODE = retrieveTraceContextMode();

    private static final SdkTracerProvider SDK_TRACER_PROVIDER = createTracerProvider();

    private static final OpenTelemetry OPEN_TELEMETRY = initializeOpenTelemetry();


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

        if (GlobalOpenTelemetry.isSet()) {
            return GlobalOpenTelemetry.get();
        }

        return createDefaultOpenTelemetry();
    }

    public static CompletableResultCode forceFlush() {
        if (GlobalOpenTelemetry.isSet()) {
            return CompletableResultCode.ofSuccess();
        }
        return SDK_TRACER_PROVIDER.forceFlush();
    }

    /**
     * Creates the Powertools default OpenTelemetry configuration.
     */
    private static OpenTelemetry createDefaultOpenTelemetry() {

        return OpenTelemetrySdk.builder()
                .setTracerProvider(SDK_TRACER_PROVIDER)
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

        SpanExporter exporter = createExporter();

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

    private static SpanExporter createExporter() {

        String protocol = SystemWrapper.getenv(OTEL_EXPORTER_OTLP_TRACES_PROTOCOL);
        String endpoint = SystemWrapper.getenv(OTEL_EXPORTER_OTLP_TRACES_ENDPOINT);
        String headers = SystemWrapper.getenv(OTEL_EXPORTER_OTLP_TRACES_HEADERS);

        Map<String, String> headerMap = parseHeaders(headers);

        if (protocol == null || protocol.isBlank()) {
            return OtlpGrpcSpanExporter.builder()
                    .setTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .setEndpoint(endpoint)
                    .setHeaders(() -> headerMap)
                    .build();
        }

        switch (protocol.trim().toLowerCase()) {
            case "grpc":
                return OtlpGrpcSpanExporter.builder()
                        .setTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                        .setEndpoint(endpoint)
                        .setHeaders(() -> headerMap)
                        .build();

            case "http/protobuf":
                return OtlpHttpSpanExporter.builder()
                        .setTimeout(EXPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                        .setEndpoint(endpoint)
                        .setHeaders(() -> headerMap)
                        .build();

            default:
                throw new IllegalArgumentException(
                        "Unsupported OTLP protocol: " + protocol
                );
        }
    }

    private static Map<String, String> parseHeaders(String headers) {

        if (headers == null || headers.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        String[] entries = headers.split(",");

        for (String entry : entries) {

            String[] parts = entry.split("=", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid OTLP header: " + entry
                );
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            if (key.isEmpty()) {
                throw new IllegalArgumentException(
                        "OTLP header name cannot be empty"
                );
            }

            result.put(key, value);
        }

        return result;
    }

    private static TextMapPropagator createPropagator() {

        return TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                AwsXrayPropagator.getInstance()
        );
    }
}