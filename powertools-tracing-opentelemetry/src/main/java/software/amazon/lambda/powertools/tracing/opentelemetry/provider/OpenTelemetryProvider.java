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

/**
 * Provides a managed OpenTelemetry instance tailored for AWS Lambda Powertools.
 * This class enables easy integration of tracing capabilities using OpenTelemetry
 * for AWS Lambda function monitoring.
 * <p>
 * It supports automatic instrumentation configuration through environment variables
 * and enables customized configurations for propagators, tracing mode, exporter,
 * and tracer provider.
 * <p>
 * OpenTelemetryProvider ensures compatibility with the ADOT Lambda layer and javaagent,
 * using the configured global OpenTelemetry instance when available. If no global
 * configuration exists, it creates and uses a Lambda-optimized configuration.
 * <p>
 * Key functionalities include:
 * - Access to a pre-configured {@code Tracer}.
 * - Support for multiple propagation formats (e.g., W3C Trace Context, AWS X-Ray).
 * - Batch span processing with configurable export batch size, queue size, and timeouts.
 * - Lambda-optimized default resource configuration.
 * - Parsing environment variables for OTLP configuration (e.g., protocol, endpoint).
 * <p>
 * This class cannot be instantiated directly and provides its functionalities
 * through static methods.
 */
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

    /**
     * Provides a pre-configured singleton instance of {@link ObjectMapper}.
     * <p>
     * This method is intended for consistent JSON processing across various
     * components by returning an {@code ObjectMapper} instance that is shared
     * across the application.
     *
     * @return A shared instance of {@link ObjectMapper}.
     */
    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Retrieves the current trace context propagation mode for OpenTelemetry tracing.
     * <p>
     * The trace context propagation mode determines how trace context is propagated
     * between spans, such as whether it uses a parent-child relationship or establishes
     * links between related spans.
     *
     * @return The current {@link TraceContextPropagationMode}, which may be either
     * {@code PARENT} or {@code LINK}, indicating the selected trace context
     * propagation strategy.
     */
    public static TraceContextPropagationMode traceContextPropagationMode() {
        return TRACE_CONTEXT_PROPAGATION_MODE;
    }

    /**
     * Retrieves a pre-configured instance of {@link Tracer} from the OpenTelemetry SDK.
     * <p>
     * The returned {@link Tracer} is associated with the specified instrumentation name,
     * enabling tracing for specific operations and contexts within the application.
     * This method leverages the global OpenTelemetry configuration, making it suitable
     * for use in environments where consistent instrumentation is required.
     *
     * @return A {@link Tracer} instance for instrumenting and generating trace data.
     */
    public static Tracer tracer() {
        return OPEN_TELEMETRY.getTracer(INSTRUMENTATION_NAME);
    }

    /**
     * Retrieves a pre-configured instance of {@link TextMapPropagator}.
     * <p>
     * The returned {@link TextMapPropagator} is configured to propagate
     * tracing context information across process boundaries. This is
     * used to encode and decode trace context in a key-value format,
     * enabling distributed tracing in various systems.
     *
     * @return A pre-configured {@link TextMapPropagator} instance for trace context propagation.
     */
    public static TextMapPropagator propagator() {
        return PROPAGATOR;
    }

    /**
     * Provides a static {@link TextMapGetter} instance for extracting trace context
     * information from a {@link Map} containing string key-value pairs.
     * <p>
     * The returned {@link TextMapGetter} is used to interpret trace propagation
     * attributes from a map structure, enabling distributed tracing functionality.
     *
     * @return A {@link TextMapGetter} instance that facilitates extracting trace
     * context data from a {@link Map} of string keys and values.
     */
    public static TextMapGetter<Map<String, String>> textMapGetter() {
        return TEXT_MAP_GETTER;
    }

    private static OpenTelemetry initializeOpenTelemetry() {

        if (GlobalOpenTelemetry.isSet()) {
            return GlobalOpenTelemetry.get();
        }

        return createDefaultOpenTelemetry();
    }

    /**
     * Forces all pending telemetry data to be processed and exported, ensuring that
     * any remaining spans or related information are handled by the OpenTelemetry
     * SDK or the globally configured OpenTelemetry instance.
     * <p>
     * If a global OpenTelemetry instance is available, the operation will immediately
     * succeed. Otherwise, it delegates the flush operation to the SDK's tracer provider.
     *
     * @return A {@link CompletableResultCode} indicating the success or failure of the
     * flush operation. It may represent an immediate success if the global
     * OpenTelemetry instance is set, or the result of flushing managed by the
     * SDK tracer provider otherwise.
     */
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