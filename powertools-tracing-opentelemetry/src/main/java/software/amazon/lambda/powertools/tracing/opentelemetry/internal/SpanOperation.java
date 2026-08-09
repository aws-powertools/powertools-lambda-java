/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Represents a functional interface that encapsulates an operation to be performed
 * within the context of an OpenTelemetry {@link Span}.
 * <p>
 * This interface provides a contract for defining custom operations that take a
 * {@link Span} as input and execute within its context. It is used in conjunction
 * with utilities that manage OpenTelemetry spans, such as the {@code withSpan} method
 * in the {@code TracingOpenTelemetry} class.
 * <p>
 * Implementations of this interface enable the customization of behavior for spans,
 * including adding events, setting attributes, or modifying the span's status.
 * <p>
 * The operation defined by the {@code execute} method can throw an exception, which
 * allows for handling of error scenarios and proper recording of exceptions in the span.
 */
@FunctionalInterface
public interface SpanOperation {

    /**
     * Executes a custom operation within the context of the provided {@link Span}.
     * This method allows for interaction with the span, such as adding events,
     * setting attributes, or manipulating its status during the operation.
     *
     * @param span the {@link Span} within whose context the operation will be executed
     * @throws Exception if an error occurs during the execution of the operation
     */
    void execute(Span span) throws Exception;
}
