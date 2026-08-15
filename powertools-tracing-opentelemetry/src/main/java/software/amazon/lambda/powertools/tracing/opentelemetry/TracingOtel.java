package software.amazon.lambda.powertools.tracing.opentelemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable OpenTelemetry tracing for the annotated method.
 * Automatically creates and manages an OpenTelemetry span for the method invocation.
 * <p>
 * This annotation allows configuration of the namespace, span name, and capture mode
 * for tracing purposes. If no explicit configuration is provided, default values are used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TracingOtel {
    /**
     * The namespace associated with the span.
     *
     * <p>If empty, the default Powertools service name is used.
     *
     * @return the namespace
     */
    String namespace() default "";

    /**
     * The name of the span.
     *
     * <p>If empty, the annotated method name is used.
     *
     * @return the span name
     */
    String spanName() default "";

    /**
     * Controls whether the method response and/or errors are captured
     * as span data.
     *
     * @return the capture mode
     */
    CaptureMode captureMode() default CaptureMode.ENVIRONMENT_VAR;
}
