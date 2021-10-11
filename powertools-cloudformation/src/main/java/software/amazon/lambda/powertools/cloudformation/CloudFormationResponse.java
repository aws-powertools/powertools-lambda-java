package software.amazon.lambda.powertools.cloudformation;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Objects;

/**
 * Client for sending responses to AWS CloudFormation custom resources by way of a response URL, which is an Amazon S3
 * pre-signed URL.
 * <p>
 * See https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-lambda-function-code-cfnresponsemodule.html
 * <p>
 * This class is thread-safe provided the SdkHttpClient instance used is also thread-safe.
 */
public class CloudFormationResponse {

    /**
     * Internal representation of the payload to be sent to the event target URL. Retains all properties of the payload
     * except for "Data". This is done so that the serialization of the non-"Data" properties and the serialization of
     * the value of "Data" can be handled by separate ObjectMappers, if need be. The former properties are dictated by
     * the custom resource but the latter is dictated by the implementor of the custom resource handler.
     */
    @SuppressWarnings("unused")
    static class ResponseBody {
        static final ObjectMapper MAPPER = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        private static final String DATA_PROPERTY_NAME = "Data";

        private final String status;
        private final String reason;
        private final String physicalResourceId;
        private final String stackId;
        private final String requestId;
        private final String logicalResourceId;
        private final boolean noEcho;

        ResponseBody(CloudFormationCustomResourceEvent event,
                     Context context,
                     Response.Status responseStatus,
                     String physicalResourceId,
                     boolean noEcho) {
            Objects.requireNonNull(event, "CloudFormationCustomResourceEvent cannot be null");
            Objects.requireNonNull(context, "Context cannot be null");
            this.physicalResourceId = physicalResourceId != null ? physicalResourceId : context.getLogStreamName();
            this.reason = "See the details in CloudWatch Log Stream: " + context.getLogStreamName();
            this.status = responseStatus == null ? Response.Status.SUCCESS.name() : responseStatus.name();
            this.stackId = event.getStackId();
            this.requestId = event.getRequestId();
            this.logicalResourceId = event.getLogicalResourceId();
            this.noEcho = noEcho;
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

        /**
         * Returns this ResponseBody as an ObjectNode with the provided JsonNode as the value of its "Data" property.
         *
         * @param dataNode the value of the "Data" property for the returned node; may be null
         * @return an ObjectNode representation of this ResponseBody and the provided dataNode
         */
        ObjectNode toObjectNode(JsonNode dataNode) {
            ObjectNode node = MAPPER.valueToTree(this);
            if (dataNode == null) {
                node.putNull(DATA_PROPERTY_NAME);
            } else {
                node.set(DATA_PROPERTY_NAME, dataNode);
            }
            return node;
        }
    }

    private final SdkHttpClient client;

    /**
     * Creates a new CloudFormationResponse that uses the provided HTTP client and default JSON serialization format.
     *
     * @param client HTTP client to use for sending requests; cannot be null
     */
    public CloudFormationResponse(SdkHttpClient client) {
        this.client = Objects.requireNonNull(client, "SdkHttpClient cannot be null");
    }

    /**
     * Forwards a response containing a custom payload to the target resource specified by the event. The payload is
     * formed from the event and context data. Status is assumed to be SUCCESS.
     *
     * @param event   custom CF resource event. Cannot be null.
     * @param context used to specify when the function and any callbacks have completed execution, or to
     *                access information from within the Lambda execution environment. Cannot be null.
     * @return the response object
     * @throws IOException                     when unable to send the request
     * @throws CustomResourceResponseException when unable to synthesize or serialize the response payload
     */
    public HttpExecuteResponse send(CloudFormationCustomResourceEvent event,
                                    Context context) throws IOException, CustomResourceResponseException {
        return send(event, context, null);
    }

    /**
     * Forwards a response containing a custom payload to the target resource specified by the event. The payload is
     * formed from the event, context, and response data.
     *
     * @param event        custom CF resource event. Cannot be null.
     * @param context      used to specify when the function and any callbacks have completed execution, or to
     *                     access information from within the Lambda execution environment. Cannot be null.
     * @param responseData response to send, e.g. a list of name-value pairs. If null, an empty success is assumed.
     * @return the response object
     * @throws IOException                     when unable to generate or send the request
     * @throws CustomResourceResponseException when unable to serialize the response payload
     */
    public HttpExecuteResponse send(CloudFormationCustomResourceEvent event,
                                    Context context,
                                    Response responseData) throws IOException, CustomResourceResponseException {
        // no need to explicitly close in-memory stream
        StringInputStream stream = responseBodyStream(event, context, responseData);
        URI uri = URI.create(event.getResponseUrl());
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

    /**
     * Returns the response body as an input stream, for supplying with the HTTP request to the custom resource.
     *
     * @throws CustomResourceResponseException if unable to generate the response stream
     */
    StringInputStream responseBodyStream(CloudFormationCustomResourceEvent event,
                                         Context context,
                                         Response resp) throws CustomResourceResponseException {
        try {
            if (resp == null) {
                ResponseBody body = new ResponseBody(event, context, Response.Status.SUCCESS, null, false);
                ObjectNode node = body.toObjectNode(null);
                return new StringInputStream(node.toString());
            } else {
                ResponseBody body = new ResponseBody(
                        event, context, resp.getStatus(), resp.getPhysicalResourceId(), resp.isNoEcho());
                ObjectNode node = body.toObjectNode(resp.getJsonNode());
                return new StringInputStream(node.toString());
            }
        } catch (RuntimeException e) {
            throw new CustomResourceResponseException("Unable to generate response body.", e);
        }
    }
}
