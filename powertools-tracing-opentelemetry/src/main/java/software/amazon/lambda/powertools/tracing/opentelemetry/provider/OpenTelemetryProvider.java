/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.provider;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import software.amazon.lambda.powertools.tracing.opentelemetry.internal.LambdaResource;

public final class OpenTelemetryProvider {

    private static final String INSTRUMENTATION_NAME =
            "aws-lambda-powertools";

    private static final int MAX_EXPORT_BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long SCHEDULE_DELAY_MILLIS = 1_000;
    private static final long EXPORT_TIMEOUT_MILLIS = 3_000;

    private static final SdkTracerProvider TRACER_PROVIDER =
            createTracerProvider();

    private static final TextMapGetter<Map<String, String>> TEXT_MAP_GETTER = new TextMapGetter<>() {

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

    private OpenTelemetryProvider() {
    }

    public static Tracer tracer() {
        return TRACER_PROVIDER.get(INSTRUMENTATION_NAME);
    }

    public static SdkTracerProvider tracerProvider() {
        return TRACER_PROVIDER;
    }

    public static TextMapPropagator propagator() {
        return createPropagator();
    }

    public static TextMapGetter<Map<String, String>> textMapGetter() {
        return TEXT_MAP_GETTER;
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

    //TODO Pending adding AWS X-RAY propagation, the library opentelemetry-aws-xray-propagator is still in alpha
    private static TextMapPropagator createPropagator() {
        return W3CTraceContextPropagator.getInstance();
    }
}