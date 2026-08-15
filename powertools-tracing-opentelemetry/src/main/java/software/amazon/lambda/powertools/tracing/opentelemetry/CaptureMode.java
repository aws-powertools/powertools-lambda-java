package software.amazon.lambda.powertools.tracing.opentelemetry;

/**
 * Defines how method responses and errors are captured by tracing.
 */
public enum CaptureMode {

    /**
     * Capture response and errors according to environment variables.
     */
    ENVIRONMENT_VAR,

    /**
     * Capture the method response.
     */
    RESPONSE,

    /**
     * Capture errors thrown by the method.
     */
    ERROR,

    /**
     * Capture both the method response and errors.
     */
    RESPONSE_AND_ERROR,

    /**
     * Disable response and error capture.
     */
    DISABLED
}