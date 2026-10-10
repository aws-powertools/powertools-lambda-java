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

/**
 * Defines a contract for extracting, enriching, and validating tracing context information
 * from AWS Lambda event objects in order to support distributed tracing.
 * <p>
 * Implementations of this interface are intended to handle specific types of AWS Lambda
 * event sources, such as SQS, SNS, DynamoDB, Kinesis, or API Gateway events. The methods
 * within this interface facilitate propagating and enriching trace information within
 * OpenTelemetry spans and contexts.
 */
public interface LambdaEventContextExtractor {

    /**
     * Determines whether the provided event is supported by this context extractor.
     *
     * @param event The AWS Lambda event to check for compatibility. Typically, this would
     *              be an event source object such as SQS, SNS, DynamoDB, Kinesis, API
     *              Gateway, or other supported AWS Lambda event types.
     * @return true if the given event type is supported by this extractor;
     * false otherwise.
     */
    boolean supports(Object event);

    /**
     * Enriches the provided OpenTelemetry span with metadata extracted from the given AWS Lambda event.
     * This method is intended to populate the span with attributes that are specific to the event type,
     * such as metadata about the source, destination, or other relevant contextual information.
     *
     * @param event The AWS Lambda event object containing the data from which span attributes are derived.
     *              This could be an event-specific object, such as an S3Event, SQS event, or API Gateway event.
     * @param span  The OpenTelemetry {@link Span} to be enriched with attributes based on the provided event.
     */
    void enrichSpan(Object event, Span span);

    /**
     * Extracts trace context information from the given AWS Lambda event to facilitate distributed tracing.
     * This method utilizes the provided `TextMapPropagator` to extract trace context information and creates
     * an {@link ExtractedTraceContext} object containing the extracted data.
     *
     * @param event         The AWS Lambda event object from which trace context should be extracted. This could be
     *                      an event-specific object like S3Event, SQS event, or API Gateway event.
     * @param parentContext The parent OpenTelemetry {@link Context} that serves as the starting point for
     *                      trace extraction. This is typically passed from the Lambda function's invocation.
     * @param propagator    A {@link TextMapPropagator} instance used to extract trace context from the event
     *                      metadata or headers.
     * @return An {@link ExtractedTraceContext} containing the extracted trace data, including the parent context,
     * span contexts, and span kind. If no trace information is found, an {@link ExtractedTraceContext}
     * with an empty list of span contexts is returned.
     */
    ExtractedTraceContext extract(Object event, Context parentContext, TextMapPropagator propagator);
}