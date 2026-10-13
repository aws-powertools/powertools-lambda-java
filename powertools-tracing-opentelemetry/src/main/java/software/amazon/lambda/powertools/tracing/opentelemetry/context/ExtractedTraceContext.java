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

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import java.util.List;

/**
 * Represents the trace context extracted during event processing in an OpenTelemetry-based tracing system.
 * This class encapsulates the parent context, a collection of span contexts, and the span kind associated
 * with the extracted trace.
 * <p>
 * Instances of this class are immutable, ensuring thread-safety when utilized in multi-threaded environments.
 */
public final class ExtractedTraceContext {

    private final Context parentContext;
    private final List<SpanContext> spanContexts;
    private final SpanKind spanKind;

    public ExtractedTraceContext(Context parentContext, List<SpanContext> spanContexts, SpanKind spanKind) {
        this.parentContext = parentContext;
        this.spanContexts = spanContexts;
        this.spanKind = spanKind;
    }

    public ExtractedTraceContext(Context parentContext, List<SpanContext> spanContexts) {
        this.parentContext = parentContext;
        this.spanContexts = spanContexts;
        this.spanKind = SpanKind.SERVER;
    }

    /**
     * Returns the parent context associated with this extracted trace context.
     * The parent context provides the linkage to the pre-existing context
     * in the OpenTelemetry system, enabling context propagation.
     *
     * @return the parent {@link Context} of this extracted trace context
     */
    public Context context() {
        return parentContext;
    }

    /**
     * Returns the collection of {@link SpanContext} instances associated with this extracted trace context.
     * Span contexts represent individual trace spans, enabling correlation and telemetry processing
     * across distributed systems.
     *
     * @return a list of {@link SpanContext} instances associated with this trace context
     */
    public List<SpanContext> spanContexts() {
        return spanContexts;
    }

    /**
     * Returns the span kind associated with this extracted trace context.
     * The span kind indicates the role of the span in a distributed trace,
     * such as SERVER, CLIENT, PRODUCER, or CONSUMER.
     *
     * @return the {@link SpanKind} of this extracted trace context
     */
    public SpanKind spanKind() {
        return spanKind;
    }
}
