/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package software.amazon.lambda.powertools.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import io.burt.jmespath.Expression;
import software.amazon.lambda.powertools.validation.internal.ValidationAspect;

/**
 * Validation utility, used to manually validate Json against Json Schema
 */
public class ValidationUtils {
    private static final String CLASSPATH = "classpath:";

    private static final ConcurrentHashMap<String, JsonSchema> schemas = new ConcurrentHashMap<>();

    private ValidationUtils() {
    }

    /**
     * Validate part of a json object against a json schema
     *
     * @param obj        The object to validate
     * @param jsonSchema The schema used to validate: either the schema itself or a path to file in the classpath: "classpath:/path/to/schema.json"
     * @param envelope   a path to a sub object within obj
     */
    public static void validate(Object obj, String jsonSchema, String envelope) throws ValidationException {
        validate(obj, getJsonSchema(jsonSchema), envelope);
    }

    /**
     * Validate part of a json object against a json schema
     *
     * @param obj        The object to validate
     * @param jsonSchema The schema used to validate
     * @param envelope   a path to a sub object within obj
     */
    public static void validate(Object obj, JsonSchema jsonSchema, String envelope) throws ValidationException {
        if (envelope == null || envelope.isEmpty()) {
            validate(obj, jsonSchema);
            return;
        }
        JsonNode subNode;
        try {
            JsonNode jsonNode = ValidationConfig.get().getObjectMapper().valueToTree(obj);
            Expression<JsonNode> expression = ValidationConfig.get().getJmesPath().compile(envelope);
            subNode = expression.search(jsonNode);
        } catch (Exception e) {
            throw new ValidationException("Cannot find envelope <"+envelope+"> in the object <"+obj+">", e);
        }
        if (subNode.getNodeType() == JsonNodeType.ARRAY) {
            subNode.forEach(jsonNode -> validate(jsonNode, jsonSchema));
        } else if (subNode.getNodeType() == JsonNodeType.OBJECT) {
            validate(subNode, jsonSchema);
        } else if (subNode.getNodeType() == JsonNodeType.STRING) {
            // try to validate as json string
            try {
                validate(subNode.asText(), jsonSchema);
            } catch (ValidationException e) {
                throw new ValidationException("Invalid format for '" + envelope + "': 'STRING' and no JSON found in it.");
            }
        } else {
            throw new ValidationException("Invalid format for '" + envelope + "': '" + subNode.getNodeType() + "'");
        }
    }

    /**
     * Validate a json object against a json schema
     *
     * @param obj        Object to validate
     * @param jsonSchema The schema used to validate: either the schema itself or a path to file in the classpath: "classpath:/path/to/schema.json"
     * @throws ValidationException if validation fails
     */
    public static void validate(Object obj, String jsonSchema) throws ValidationException {
        validate(obj, getJsonSchema(jsonSchema));
    }

    /**
     * Validate a json object against a json schema
     *
     * @param obj        Object to validate
     * @param jsonSchema The schema used to validate
     * @throws ValidationException if validation fails
     */
    public static void validate(Object obj, JsonSchema jsonSchema) throws ValidationException {
        JsonNode jsonNode;
        try {
            jsonNode = ValidationConfig.get().getObjectMapper().valueToTree(obj);
        } catch (Exception e) {
            throw new ValidationException("Object <"+obj+"> is not valid against the schema provided", e);
        }

        validate(jsonNode, jsonSchema);
    }

    /**
     * Validate a json object (in string format) against a json schema
     *
     * @param json       Json in string format
     * @param jsonSchema The schema used to validate: either the schema itself or a path to file in the classpath: "classpath:/path/to/schema.json"
     * @throws ValidationException if validation fails
     */
    public static void validate(String json, String jsonSchema) throws ValidationException {
        validate(json, getJsonSchema(jsonSchema));
    }

    /**
     * Validate a json object (in string format) against a json schema
     *
     * @param json       json in string format
     * @param jsonSchema the schema used to validate json string
     * @throws ValidationException if validation fails
     */
    public static void validate(String json, JsonSchema jsonSchema) throws ValidationException {
        JsonNode jsonNode;
        try {
            jsonNode = ValidationConfig.get().getObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new ValidationException("Json <"+json+"> is not valid against the schema provided", e);
        }

        validate(jsonNode, jsonSchema);
    }

    /**
     * Validate a json object (in map format) against a json schema
     *
     * @param map        Map to be transformed in json and validated against the schema
     * @param jsonSchema The schema used to validate: either the schema itself or a path to file in the classpath: "classpath:/path/to/schema.json"
     * @throws ValidationException if validation fails
     */
    public static void validate(Map<String, Object> map, String jsonSchema) throws ValidationException {
        validate(map, getJsonSchema(jsonSchema));
    }

    /**
     * Validate a json object (in map format) against a json schema
     *
     * @param map        Map to be transformed in json and validated against the schema
     * @param jsonSchema the schema used to validate json map
     * @throws ValidationException if validation fails
     */
    public static void validate(Map<String, Object> map, JsonSchema jsonSchema) throws ValidationException {
        JsonNode jsonNode;
        try {
            jsonNode = ValidationConfig.get().getObjectMapper().valueToTree(map);
        } catch (Exception e) {
            throw new ValidationException("Map <"+map+"> cannot be converted to json for validation", e);
        }

        validate(jsonNode, jsonSchema);
    }

    /**
     * Validate a json object (in JsonNode format) against a json schema.<br>
     * Perform the actual validation.
     *
     * @param jsonNode   Json to be validated against the schema
     * @param jsonSchema The schema used to validate: either the schema itself or a path to file in the classpath: "classpath:/path/to/schema.json"
     * @throws ValidationException if validation fails
     */
    public static void validate(JsonNode jsonNode, String jsonSchema) throws ValidationException {
        validate(jsonNode, getJsonSchema(jsonSchema));
    }

    /**
     * Validate a json object (in JsonNode format) against a json schema.<br>
     * Perform the actual validation.
     *
     * @param jsonNode   json to be validated against the schema
     * @param jsonSchema the schema to validate json node
     * @throws ValidationException if validation fails
     */
    public static void validate(JsonNode jsonNode, JsonSchema jsonSchema) throws ValidationException {
        Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode);
        if (!validationMessages.isEmpty()) {
            String message;
            try {
                message = ValidationConfig.get().getObjectMapper().writeValueAsString(new ValidationErrors(validationMessages));
            } catch (JsonProcessingException e) {
                message = validationMessages.stream().map(ValidationMessage::getMessage).collect(Collectors.joining(", "));
            }
            throw new ValidationException(message);
        }
    }

    /**
     * Retrieve {@link JsonSchema} from string (either the schema itself, either from the classpath).<br/>
     * No validation of the schema will be performed (equivalent to <pre>getJsonSchema(schema, false)</pre><br/>
     * Store it in memory to avoid reloading it.<br/>
     *
     * @param schema either the schema itself of a "classpath:/path/to/schema.json"
     * @return the loaded json schema
     */
    public static JsonSchema getJsonSchema(String schema) {
        return getJsonSchema(schema, false);
    }

    /**
     * Retrieve {@link JsonSchema} from string (either the schema itself, either from the classpath).<br/>
     * Optional: validate the schema against the version specifications.<br/>
     * Store it in memory to avoid reloading it.<br/>
     *
     * @param schema         either the schema itself of a "classpath:/path/to/schema.json"
     * @param validateSchema specify if the schema itself must be validated against specifications
     * @return the loaded json schema
     */
    public static JsonSchema getJsonSchema(String schema, boolean validateSchema) {
        JsonSchema jsonSchema = schemas.get(schema);

        if (jsonSchema != null) {
            return jsonSchema;
        }

        if (schema.startsWith(CLASSPATH)) {
            String filePath = schema.substring(CLASSPATH.length());
            try (InputStream schemaStream = ValidationAspect.class.getResourceAsStream(filePath)) {
                if (schemaStream == null) {
                    throw new IllegalArgumentException("'" + schema + "' is invalid, verify '" + filePath + "' is in your classpath");
                }

                jsonSchema = ValidationConfig.get().getFactory().getSchema(schemaStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("'" + schema + "' is invalid, verify '" + filePath + "' is in your classpath");
            }
        } else {
            jsonSchema = ValidationConfig.get().getFactory().getSchema(schema);
        }

        if (validateSchema) {
            String version = ValidationConfig.get().getSchemaVersion().toString();
            try {
                validate(jsonSchema.getSchemaNode(),
                        getJsonSchema("classpath:/schemas/meta_schema_" + version));
            } catch (ValidationException ve) {
                throw new IllegalArgumentException("The schema " + schema + " is not valid, it does not respect the specification " + version, ve);
            }
        }

        schemas.put(schema, jsonSchema);

        return jsonSchema;
    }

    /**
     *
     */
    public static class ValidationErrors {

        private final Set<ValidationMessage> validationErrors;

        private ValidationErrors(Set<ValidationMessage> validationErrors) {
            this.validationErrors = validationErrors;
        }

        public Set<ValidationMessage> getValidationErrors() {
            return Collections.unmodifiableSet(validationErrors);
        }
    }
}
