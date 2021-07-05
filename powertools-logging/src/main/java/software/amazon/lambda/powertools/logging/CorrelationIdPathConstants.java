package software.amazon.lambda.powertools.logging;

/**
 * Supported Event types from which Correlation ID can be extracted
 */
public class CorrelationIdPathConstants {
    /**
     * To use when function is expecting API Gateway Rest API Request event
     */
    public static final String API_GATEWAY_REST = "/requestContext/requestId";
    /**
     * To use when function is expecting API Gateway HTTP API Request event
     */
    public static final String  API_GATEWAY_HTTP = "/requestContext/requestId";
    /**
     * To use when function is expecting Application Load balancer Request event
     */
    public static final String APPLICATION_LOAD_BALANCER = "/headers/x-amzn-trace-id";
    /**
     * To use when function is expecting Event Bridge Request event
     */
    public static final String  EVENT_BRIDGE = "/id";
}
