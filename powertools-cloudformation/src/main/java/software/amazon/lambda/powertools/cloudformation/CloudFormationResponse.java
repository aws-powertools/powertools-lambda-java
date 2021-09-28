package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.StringInputStream;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for sending responses to AWS CloudFormation custom resources by way of a response URL, which is an Amazon S3
 * pre-signed URL.
 * <p>
 * See https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-lambda-function-code-cfnresponsemodule.html
 */
public class CloudFormationResponse {

    /**
     * Indicates if the function invoking send had successfully completed.
     */
    public enum ResponseStatus {
        SUCCESS, FAILED
    }

    /**
     * Representation of the payload to be sent to the event target URL.
     */
    @SuppressWarnings("unused")
    static class ResponseBody {
        private final String status;
        private final String reason;
        private final String physicalResourceId;
        private final String stackId;
        private final String requestId;
        private final String logicalResourceId;
        private final boolean noEcho;
        private final Object data;

        ResponseBody(CloudFormationCustomResourceEvent event,
                     Context context,
                     ResponseStatus responseStatus,
                     Object responseData,
                     String physicalResourceId,
                     boolean noEcho) {
            requireNonNull(event, "CloudFormationCustomResourceEvent");
            requireNonNull(context, "Context");
            this.physicalResourceId = physicalResourceId != null ? physicalResourceId : context.getLogStreamName();
            this.reason = "See the details in CloudWatch Log Stream: " + context.getLogStreamName();
            this.status = responseStatus == null ? ResponseStatus.SUCCESS.name() : responseStatus.name();
            this.stackId = event.getStackId();
            this.requestId = event.getRequestId();
            this.logicalResourceId = event.getLogicalResourceId();
            this.noEcho = noEcho;
            this.data = responseData;
        }

        public String getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public String getPhysicalResourceId() {
            return physicalResourceId;
        }

        public String getStackId() {
            return stackId;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getLogicalResourceId() {
            return logicalResourceId;
        }

        public boolean isNoEcho() {
            return noEcho;
        }

        public Object getData() {
            return data;
        }
    }

    /*
     * Replace with java.util.Objects::requireNonNull when dropping JDK 8 support.
     */
    private static <T> T requireNonNull(T arg, String argName) {
        if (arg == null) {
            throw new NullPointerException(argName + " cannot be null.");
        }
        return arg;
    }

    private final SdkHttpClient client;
    private final ObjectMapper mapper;

    /**
     * Creates a new CloudFormationResponse that uses the provided HTTP client and default JSON serialization format.
     *
     * @param client HTTP client to use for sending requests; cannot be null
     */
    public CloudFormationResponse(SdkHttpClient client) {
        this(client, new ObjectMapper()
                .setPropertyNamingStrategy(new PropertyNamingStrategies.UpperCamelCaseStrategy()));
    }

    /**
     * Creates a new CloudFormationResponse that uses the provided HTTP client and the JSON serialization mapper object.
     *
     * @param client HTTP client to use for sending requests; cannot be null
     * @param mapper for serializing response data; cannot be null
     */
    protected CloudFormationResponse(SdkHttpClient client, ObjectMapper mapper) {
        this.client = requireNonNull(client, "SdkHttpClient");
        this.mapper = requireNonNull(mapper, "ObjectMapper");
    }

    /**
     * Forwards a request containing a custom payload to the target resource specified by the event. The payload is
     * formed from the event and context data.
     *
     * @param event   custom CF resource event
     * @param context used to specify when the function and any callbacks have completed execution, or to
     *                access information from within the Lambda execution environment.
     * @param status  whether the function successfully completed
     * @return the response object
     * @throws IOException when unable to generate or send the request
     */
    public HttpExecuteResponse send(CloudFormationCustomResourceEvent event,
                                    Context context,
                                    ResponseStatus status) throws IOException {
        return send(event, context, status, null);
    }

    /**
     * Forwards a request containing a custom payload to the target resource specified by the event. The payload is
     * formed from the event, status, and context data.
     *
     * @param event        custom CF resource event
     * @param context      used to specify when the function and any callbacks have completed execution, or to
     *                     access information from within the Lambda execution environment.
     * @param status       whether the function successfully completed
     * @param responseData response data, e.g. a list of name-value pairs. May be null.
     * @return the response object
     * @throws IOException when unable to generate or send the request
     */
    public HttpExecuteResponse send(CloudFormationCustomResourceEvent event,
                                    Context context,
                                    ResponseStatus status,
                                    Object responseData) throws IOException {
        return send(event, context, status, responseData, null);
    }

    /**
     * Forwards a request containing a custom payload to the target resource specified by the event. The payload is
     * formed from the event, status, response, and context data.
     *
     * @param event              custom CF resource event
     * @param context            used to specify when the function and any callbacks have completed execution, or to
     *                           access information from within the Lambda execution environment.
     * @param status             whether the function successfully completed
     * @param responseData       response data, e.g. a list of name-value pairs. May be null.
     * @param physicalResourceId Optional. The unique identifier of the custom resource that invoked the function. By
     *                           default, the module uses the name of the Amazon CloudWatch Logs log stream that's
     *                           associated with the Lambda function.
     * @return the response object
     * @throws IOException when unable to generate or send the request
     */
    public HttpExecuteResponse send(CloudFormationCustomResourceEvent event,
                                    Context context,
                                    ResponseStatus status,
                                    Object responseData,
                                    String physicalResourceId) throws IOException {
        return send(event, context, status, responseData, physicalResourceId, false);
    }

    /**
     * Forwards a request containing a custom payload to the target resource specified by the event. The payload is
     * formed from the event, status, response, and context data.
     *
     * @param event              custom CF resource event
     * @param context            used to specify when the function and any callbacks have completed execution, or to
     *                           access information from within the Lambda execution environment.
     * @param status             whether the function successfully completed
     * @param responseData       response data, e.g. a list of name-value pairs. May be null.
     * @param physicalResourceId Optional. The unique identifier of the custom resource that invoked the function. By
     *                           default, the module uses the name of the Amazon CloudWatch Logs log stream that's
     *                           associated with the Lambda function.
     * @param noEcho             Optional. Indicates whether to mask the output of the custom resource when it's
     *                           retrieved by using the Fn::GetAtt function. If set to true, all returned values are
     *                           masked with asterisks (*****), except for information stored in the locations specified
     *                           below. By default, this value is false.
     * @return the response object
     * @throws IOException when unable to generate or send the request
     */
    public HttpExecuteResponse send(CloudFormationCustomResourceEvent event,
                                    Context context,
                                    ResponseStatus status,
                                    Object responseData,
                                    String physicalResourceId,
                                    boolean noEcho) throws IOException {
        ResponseBody body = new ResponseBody(event, context, status, responseData, physicalResourceId, noEcho);
        URI uri = URI.create(event.getResponseUrl());

        // no need to explicitly close in-memory stream
        StringInputStream stream = new StringInputStream(mapper.writeValueAsString(body));
        SdkHttpRequest request = SdkHttpRequest.builder()
                .uri(uri)
                .method(SdkHttpMethod.PUT)
                .headers(headers(stream.available()))
                .build();
        HttpExecuteRequest httpExecuteRequest = HttpExecuteRequest.builder()
                .request(request)
                .contentStreamProvider(() -> stream)
                .build();
        return client.prepareRequest(httpExecuteRequest).call();
    }

    /**
     * Generates HTTP headers to be supplied in the CloudFormation request.
     *
     * @param contentLength the length of the payload
     * @return HTTP headers
     */
    protected Map<String, List<String>> headers(int contentLength) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(Header.CONTENT_TYPE, Collections.emptyList()); // intentionally empty
        headers.put(Header.CONTENT_LENGTH, Collections.singletonList(Integer.toString(contentLength)));
        return headers;
    }
}
