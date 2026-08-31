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

package software.amazon.lambda.powertools.tracing.opentelemetry.context;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;

/**
 * Resolves and delegates processing of Lambda-specific event contexts to the appropriate
 * {@link LambdaEventContextExtractor} implementation based on the event type.
 * This resolver allows for dynamic extraction and span enrichment tailored to
 * various AWS Lambda event sources (e.g., API Gateway, SQS, SNS, etc.).
 * <p>
 * This class is immutable and thread-safe.
 */
public final class LambdaEventContextExtractorResolver {

    private final List<LambdaEventContextExtractor> extractors;

    public LambdaEventContextExtractorResolver(List<LambdaEventContextExtractor> extractors) {

        this.extractors = List.copyOf(extractors);
    }

    /**
     * Creates and returns an instance of {@link LambdaEventContextExtractorResolver} configured with
     * a predefined set of {@link LambdaEventContextExtractor} implementations. These extractors are specialized in
     * processing different types of AWS Lambda event sources, such as API Gateway, SQS, SNS, Kinesis, DynamoDB, and S3.
     *
     * @return a new instance of {@link LambdaEventContextExtractorResolver} with predefined extractors for
     * handling various AWS Lambda event contexts.
     */
    public static LambdaEventContextExtractorResolver create() {
        return new LambdaEventContextExtractorResolver(
                List.of(
                        new ApiGatewayTraceContextExtractor(),
                        new SqsTraceContextExtractor(),
                        new SnsTraceContextExtractor(),
                        new KinesisTraceContextExtractor(),
                        new DynamoDbTraceContextExtractor(),
                        new S3TraceContextExtractor()
                )
        );
    }

    /**
     * Extracts trace context information from a Lambda event using the appropriate
     * {@link LambdaEventContextExtractor} implementation that supports the event type.
     * This method delegates the extraction to the first extractor in the configured list
     * that supports the provided event type. If no suitable extractor is found, a default
     * {@link ExtractedTraceContext} is returned using the provided parent context.
     *
     * @param event         the Lambda event from which to extract the trace context
     * @param parentContext the parent {@link Context} to be used as the base for the extraction
     * @param propagator    the {@link TextMapPropagator} used to extract propagation information from the event
     * @return an {@link ExtractedTraceContext} containing the extracted trace context or a default one
     * if no supporting extractor is found
     */
    public ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator) {

        return extractors.stream()
                .filter(extractor -> extractor.supports(event))
                .findFirst()
                .map(extractor ->
                        extractor.extract(
                                event,
                                parentContext,
                                propagator))
                .orElse(new ExtractedTraceContext(parentContext, List.of()));
    }

    /**
     * Enriches a given {@link Span} with contextual information extracted from
     * the specified event. This method evaluates a list of configured extractors
     * and delegates the enrichment process to the first extractor that supports
     * the provided event type.
     *
     * @param event the event object containing context information to be added to the span
     * @param span  the {@link Span} instance to be enriched with extracted information
     */
    public void enrichSpan(Object event, Span span) {
        extractors.stream()
                .filter(extractor -> extractor.supports(event))
                .findFirst()
                .ifPresent(extractor -> extractor.enrichSpan(event, span));
    }
}
