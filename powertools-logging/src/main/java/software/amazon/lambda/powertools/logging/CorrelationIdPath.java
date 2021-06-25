package software.amazon.lambda.powertools.logging;

/**
 * Supported Event types from which Correlation ID can be extracted
 */
public enum CorrelationIdPath {
    /**
     * To use when function is expecting API Gateway Rest API Request event
     */
    API_GATEWAY_REST("/requestContext/requestId"),
    /**
     * To use when function is expecting API Gateway HTTP API Request event
     */
    API_GATEWAY_HTTP("/requestContext/requestId"),
    /**
     * To use when function is expecting Application Load balancer Request event
     */
    APPLICATION_LOAD_BALANCER("/headers/x-amzn-trace-id"),
    /**
     * To use when function is expecting Event Bridge Request event
     */
    EVENT_BRIDGE("/id"),
    /**
     * To use when function is expecting any of the supported event types to automatically
     * extract the correlation id. When running in this mode, if its unable to find any of the
     * supported types, capturing of correlation id will not be done.
     */
    AUTO_DETECT(""),
    /**
     * To use when you dont want to extract correlation id.
     */
    DISABLED("");

    private final String path;


    CorrelationIdPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
