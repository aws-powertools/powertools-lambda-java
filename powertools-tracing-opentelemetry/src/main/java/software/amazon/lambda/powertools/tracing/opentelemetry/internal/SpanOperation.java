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

import io.opentelemetry.api.trace.Span;

/**
 * Represents a functional interface used to execute a custom operation within
 * the context of a given {@link Span}. This interface requires implementing a
 * single method that performs an operation with the span and optionally
 * returns a result.
 *
 * <p>
 * The {@code SpanOperation} interface enables tracing and manipulation of
 * a span during its lifecycle, such as setting attributes, adding events,
 * or updating status codes. It can be used alongside frameworks that support
 * OpenTelemetry for distributed tracing.
 *
 * @param <T> the type of result returned by the custom span operation
 */
@FunctionalInterface
public interface SpanOperation<T> {

    /**
     * Executes a custom operation within the context of the provided {@link Span}.
     * This method allows for interaction with the span, such as adding events,
     * setting attributes, or manipulating its status during the operation.
     *
     * @param span the {@link Span} within whose context the operation will be executed
     * @throws Exception if an error occurs during the execution of the operation
     */
    T execute(Span span) throws Exception;
}
