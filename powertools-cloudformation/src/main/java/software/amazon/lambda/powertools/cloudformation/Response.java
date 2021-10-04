package software.amazon.lambda.powertools.cloudformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Models the arbitrary data to be sent to the custom resource in response to a CloudFormation event. This object
 * encapsulates the data and the means to serialize it.
 */
public class Response {

    /**
     * For building Response instances that wrap a single, arbitrary value.
     */
    public static class Builder {
        private Object value;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        /**
         * Configures a custom ObjectMapper for serializing the value object.
         *
         * @param value cannot be null
         * @return a reference to this builder
         */
        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        /**
         * Configures a custom ObjectMapper for serializing the value object.
         *
         * @param objectMapper if null, a default mapper will be used
         * @return a reference to this builder
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Builds a Response object for the value.
         *
         * @return a Response object wrapping the initially provided value.
         */
        public Response build() {
            Object val = Objects.requireNonNull(value, "A non-null value is required");
            ObjectMapper mapper = objectMapper != null ? objectMapper : CloudFormationResponse.ResponseBody.MAPPER;
            JsonNode node = mapper.valueToTree(val);
            return new Response(node);
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

    private final JsonNode jsonNode;

    private Response(final JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    /**
     * Returns a JsonNode representation of the response.
     *
     * @return a non-null JsonNode representation
     */
    JsonNode getJsonNode() {
        return jsonNode;
    }

    /**
     * The Response JSON.
     *
     * @return a String in JSON format
     */
    @Override
    public String toString() {
        return jsonNode.toString();
    }
}
