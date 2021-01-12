package software.amazon.lambda.powertools.tracing;

public enum CaptureMode {
    /**
     * Enables annotation to capture only response. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override value of environment variable POWERTOOLS_TRACER_CAPTURE_RESPONSE
     */
    RESPONSE,
    /**
     * Enabled annotation to capture only error from the method. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override value of environment variable POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    ERROR,
    /**
     * Enabled annotation to capture both response error from the method. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override value of environment variables POWERTOOLS_TRACER_CAPTURE_RESPONSE
     * and POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    RESPONSE_AND_ERROR,
    /**
     * Disables annotation to capture both response and error from the method. If this mode is explicitly overridden
     * on {@link Tracing} annotation, it will override values of environment variable POWERTOOLS_TRACER_CAPTURE_RESPONSE
     * and POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    DISABLED,
    /**
     * Enables/Disables annotation to capture response and error from the method based on the value of
     * environment variable POWERTOOLS_TRACER_CAPTURE_RESPONSE and POWERTOOLS_TRACER_CAPTURE_ERROR
     */
    ENVIRONMENT_VAR
}
