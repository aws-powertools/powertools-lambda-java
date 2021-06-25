package software.amazon.lambda.powertools.logging;

public enum CorrelationIdPath {
    API_GATEWAY_REST("/requestContext/requestId"),
    API_GATEWAY_HTTP("/requestContext/requestId"),
    APPLICATION_LOAD_BALANCER("/headers/x-amzn-trace-id"),
    EVENT_BRIDGE("/id"),
    AUTO_DETECT(""),
    DISABLED("");

    private final String path;


    CorrelationIdPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
