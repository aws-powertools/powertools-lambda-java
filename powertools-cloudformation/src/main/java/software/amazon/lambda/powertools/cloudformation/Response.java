package software.amazon.lambda.powertools.cloudformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Models the arbitrary data to be sent to the custom resource in response to a CloudFormation event. This object
 * encapsulates the data and the means to serialize it.
 */
public class Response {

    /**
     * Indicates whether a response is a success or failure.
     */
    public enum Status {
        SUCCESS, FAILED
    }

    /**
     * For building Response instances.
     */
    public static class Builder {
        private Object value;
        private ObjectMapper objectMapper;
        private Status status;
        private String physicalResourceId;
        private boolean noEcho;

        private Builder() {
        }

        /**
         * Configures the value of this Response, typically a Map of name/value pairs.
         *
         * @param value if null, the Response will be empty
         * @return a reference to this builder
         */
        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        /**
         * Configures a custom ObjectMapper for serializing the value object. Creates a copy of the mapper provided;
         * future mutations of the ObjectMapper made using the provided reference will not affect Response
         * serialization.
         *
         * @param objectMapper if null, a default mapper will be used
         * @return a reference to this builder
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper == null ? null : objectMapper.copy();
            return this;
        }

        /**
         * Configures the status of this response.
         *
         * @param status if null, SUCCESS will be assumed
         * @return a reference to this builder
         */
        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        /**
         * A unique identifier for the custom resource being responded to. By default, the identifier is the name of the
         * Amazon CloudWatch Logs log stream associated with the Lambda function.
         *
         * @param physicalResourceId if null, the default resource ID will be used
         * @return a reference to this builder
         */
        public Builder physicalResourceId(String physicalResourceId) {
            this.physicalResourceId = physicalResourceId;
            return this;
        }

        /**
         * Indicates whether to mask the output of the custom resource when it's retrieved by using the Fn::GetAtt
         * function. If set to true, values will be masked with asterisks (*****), except for information stored in the
         * these locations:
         * <ul>
         * <li>The Metadata template section. CloudFormation does not transform, modify, or redact any information
         * included in the Metadata section. See
         * https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/metadata-section-structure.html</li>
         * <li>The Outputs template section. See
         * https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/outputs-section-structure.html</li>
         * <li>The Metadata attribute of a resource definition. See
         * https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-attribute-metadata.html</li>
         * </ul>
         * <p>
         * We strongly recommend not using these mechanisms to include sensitive information, such as passwords or
         * secrets.
         * <p>
         * For more information about using noEcho to mask sensitive information, see
         * https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/best-practices.html#creds
         * <p>
         * By default, this value is false.
         *
         * @param noEcho when true, masks certain output
         * @return a reference to this builder
         */
        public Builder noEcho(boolean noEcho) {
            this.noEcho = noEcho;
            return this;
        }

        /**
         * Builds a Response object for the value.
         *
         * @return a Response object wrapping the initially provided value.
         */
        public Response build() {
            JsonNode node;
            if (value == null) {
                node = null;
            } else {
                ObjectMapper mapper = objectMapper != null ? objectMapper : CloudFormationResponse.ResponseBody.MAPPER;
                node = mapper.valueToTree(value);
            }
            Status responseStatus = this.status != null ? this.status : Status.SUCCESS;
            return new Response(node, responseStatus, physicalResourceId, noEcho);
        }
    }

    /**
     * Creates a builder for constructing a Response wrapping the provided value.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a failed Response with no physicalResourceId set. Powertools will set the physicalResourceId to the
     * Lambda LogStreamName
     *
     * The value returned for a PhysicalResourceId can change custom resource update operations. If the value returned
     * is the same, it is considered a normal update. If the value returned is different, AWS CloudFormation recognizes
     * the update as a replacement and sends a delete request to the old resource. For more information,
     * see AWS::CloudFormation::CustomResource.
     *
     * @return a failed Response with no value.
     */
    @Deprecated
    public static Response failed() {
        return new Response(null, Status.FAILED, null, false);
    }

    /**
     * Creates a failed Response with a given physicalResourceId.
     *
     * @param physicalResourceId The value must be a non-empty string and must be identical for all responses for the
     *                           same resource.
     *                           The value returned for a PhysicalResourceId can change custom resource update
     *                           operations. If the value returned is the same, it is considered a normal update. If the
     *                           value returned is different, AWS CloudFormation recognizes the update as a replacement
     *                           and sends a delete request to the old resource. For more information,
     *                           see AWS::CloudFormation::CustomResource.
     * @return a failed Response with physicalResourceId
     */
    public static Response failed(String physicalResourceId) {
        return new Response(null, Status.FAILED, physicalResourceId, false);
    }

    /**
     * Creates a successful Response with no physicalResourceId set. Powertools will set the physicalResourceId to the
     * Lambda LogStreamName
     *
     * The value returned for a PhysicalResourceId can change custom resource update operations. If the value returned
     * is the same, it is considered a normal update. If the value returned is different, AWS CloudFormation recognizes
     * the update as a replacement and sends a delete request to the old resource. For more information,
     * see AWS::CloudFormation::CustomResource.
     *
     * @return a success Response with no physicalResourceId value.
     */
    @Deprecated
    public static Response success() {
        return new Response(null, Status.SUCCESS, null, false);
    }

    /**
     * Creates a successful Response with a given physicalResourceId.
     *
     * @param physicalResourceId The value must be a non-empty string and must be identical for all responses for the
     *                           same resource.
     *                           The value returned for a PhysicalResourceId can change custom resource update
     *                           operations. If the value returned is the same, it is considered a normal update. If the
     *                           value returned is different, AWS CloudFormation recognizes the update as a replacement
     *                           and sends a delete request to the old resource. For more information,
     *                           see AWS::CloudFormation::CustomResource.
     * @return a success Response with physicalResourceId
     */
    public static Response success(String physicalResourceId) {
        return new Response(null, Status.SUCCESS, physicalResourceId, false);
    }

    private final JsonNode jsonNode;
    private final Status status;
    private final String physicalResourceId;
    private final boolean noEcho;

    private Response(JsonNode jsonNode, Status status, String physicalResourceId, boolean noEcho) {
        this.jsonNode = jsonNode;
        this.status = status;
        this.physicalResourceId = physicalResourceId;
        this.noEcho = noEcho;
    }

    /**
     * Returns a JsonNode representation of the Response.
     *
     * @return a non-null JsonNode representation
     */
    JsonNode getJsonNode() {
        return jsonNode;
    }

    /**
     * The success/failed status of the Response.
     *
     * @return a non-null Status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * The physical resource ID. If null, the default physical resource ID will be provided to the custom resource.
     *
     * @return a potentially null physical resource ID
     */
    public String getPhysicalResourceId() {
        return physicalResourceId;
    }

    /**
     * Whether to mask custom resource output (true) or not (false).
     *
     * @return true if custom resource output is to be masked, false otherwise
     */
    public boolean isNoEcho() {
        return noEcho;
    }

    /**
     * Includes all Response attributes, including its value in JSON format
     *
     * @return a full description of the Response
     */
    @Override
    public String toString() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("JSON", jsonNode == null ? null : jsonNode.toString());
        attributes.put("Status", status);
        attributes.put("PhysicalResourceId", physicalResourceId);
        attributes.put("NoEcho", noEcho);
        return attributes.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(",", "[", "]"));
    }
}
