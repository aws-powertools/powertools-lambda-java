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
package software.amazon.lambda.powertools.validation.internal;

import com.amazonaws.services.lambda.runtime.events.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.validation.ValidationException;
import software.amazon.lambda.powertools.validation.Validator;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;

/**
 * Aspect for {@link Validator} annotation
 */
@Aspect
public class ValidatorAspect {

    private static final String CLASSPATH = "classpath:";

    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(validator)")
    public void callAt(Validator validator) {
    }

    @Around(value = "callAt(validator) && execution(@Validator * *.*(..))", argNames = "pjp,validator")
    public Object around(ProceedingJoinPoint pjp,
                         Validator validator) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        boolean validationNeeded = false;

        if (isHandlerMethod(pjp)
                && placedOnRequestHandler(pjp)) {
            validationNeeded = true;

            if (!validator.inboundSchema().isEmpty()) {
                JsonSchema inboundJsonSchema = getJsonSchema(validator.inboundSchema());

                Object obj = pjp.getArgs()[0];
                if (obj instanceof APIGatewayProxyRequestEvent) {
                    APIGatewayProxyRequestEvent event = (APIGatewayProxyRequestEvent) obj;
                    validate(event.getBody(), inboundJsonSchema);
                } else if (obj instanceof APIGatewayV2HTTPEvent) {
                    APIGatewayV2HTTPEvent event = (APIGatewayV2HTTPEvent) obj;
                    validate(event.getBody(), inboundJsonSchema);
                } else if (obj instanceof SNSEvent) {
                    SNSEvent event = (SNSEvent) obj;
                    event.getRecords().forEach(record -> validate(record.getSNS().getMessage(), inboundJsonSchema));
                } else if (obj instanceof SQSEvent) {
                    SQSEvent event = (SQSEvent) obj;
                    event.getRecords().forEach(record -> validate(record.getBody(), inboundJsonSchema));
                } else if (obj instanceof ScheduledEvent) {
                    ScheduledEvent event = (ScheduledEvent) obj;
                    validate(event.getDetail(), inboundJsonSchema);
                } else if (obj instanceof ApplicationLoadBalancerRequestEvent) {
                    ApplicationLoadBalancerRequestEvent event = (ApplicationLoadBalancerRequestEvent) obj;
                    validate(event.getBody(), inboundJsonSchema);
                } else if (obj instanceof CloudFormationCustomResourceEvent) {
                    CloudFormationCustomResourceEvent event = (CloudFormationCustomResourceEvent) obj;
                    validate(event.getResourceProperties(), inboundJsonSchema);
                } else if (obj instanceof KinesisEvent) {
                    KinesisEvent event = (KinesisEvent) obj;
                    event.getRecords().forEach(record -> validate(new String(record.getKinesis().getData().asReadOnlyBuffer().array()), inboundJsonSchema));
                } else if (obj instanceof KinesisFirehoseEvent) {
                    KinesisFirehoseEvent event = (KinesisFirehoseEvent) obj;
                    event.getRecords().forEach(record -> validate(new String(record.getData().asReadOnlyBuffer().array()), inboundJsonSchema));
                } else if (obj instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
                    KinesisAnalyticsFirehoseInputPreprocessingEvent event = (KinesisAnalyticsFirehoseInputPreprocessingEvent) obj;
                    event.getRecords().forEach(record -> validate(new String(record.getData().asReadOnlyBuffer().array()), inboundJsonSchema));
                } else if (obj instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
                    KinesisAnalyticsStreamsInputPreprocessingEvent event = (KinesisAnalyticsStreamsInputPreprocessingEvent) obj;
                    event.getRecords().forEach(record -> validate(new String(record.getData().asReadOnlyBuffer().array()), inboundJsonSchema));
                }
            }
        }

        Object result = pjp.proceed(proceedArgs);

        if (validationNeeded && !validator.outboundSchema().isEmpty()) {
            JsonSchema outboundJsonSchema = getJsonSchema(validator.outboundSchema());

            if (result instanceof APIGatewayProxyResponseEvent) {
                APIGatewayProxyResponseEvent response = (APIGatewayProxyResponseEvent) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof APIGatewayV2HTTPResponse) {
                APIGatewayV2HTTPResponse response = (APIGatewayV2HTTPResponse) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof APIGatewayV2WebSocketResponse) {
                APIGatewayV2WebSocketResponse response = (APIGatewayV2WebSocketResponse) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof ApplicationLoadBalancerResponseEvent) {
                ApplicationLoadBalancerResponseEvent response = (ApplicationLoadBalancerResponseEvent) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof KinesisAnalyticsInputPreprocessingResponse) {
                KinesisAnalyticsInputPreprocessingResponse response = (KinesisAnalyticsInputPreprocessingResponse) result;
                response.getRecords().forEach(record -> validate(new String(record.getData().asReadOnlyBuffer().array()), outboundJsonSchema));
            }
        }

        return result;
    }

    /**
     * Retrieve {@link JsonSchema} from string (either the schema itself, either from the classpath)
     * @param schema either the schema itself of a "classpath:/path/to/schema.json"
     * @return the loaded json schema
     */
    private JsonSchema getJsonSchema(String schema) {
        JsonSchema jsonSchema;
        if (schema.startsWith(CLASSPATH)) {
            String filePath = schema.substring(CLASSPATH.length());
            jsonSchema = factory.getSchema(ValidatorAspect.class.getResourceAsStream(filePath));
        } else {
            jsonSchema = factory.getSchema(schema);
        }
        return jsonSchema;
    }

    /**
     * Validate a json object (in string format) against a json schema
     *
     * @param json json in string format
     * @param jsonSchema the schema to validate json string
     * @throws ValidationException if validation fails
     */
    private void validate(String json, JsonSchema jsonSchema) throws ValidationException {
        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ValidationException(e);
        }

        validate(jsonNode, jsonSchema);
    }

    /**
     * Validate a json object (in map format) against a json schema
     *
     * @param map map to be transformed in json and validated against the schema
     * @param jsonSchema the schema to validate json map
     * @throws ValidationException if validation fails
     */
    private void validate(Map<String, Object> map, JsonSchema jsonSchema) throws ValidationException {
        JsonNode jsonNode;
        try {
            jsonNode = mapper.valueToTree(map);
        } catch (Exception e) {
            throw new ValidationException(e);
        }

        validate(jsonNode, jsonSchema);
    }

    /**
     * Validate a json object (in JsonNode format) against a json schema.<br>
     * Perform the actual validation.
     *
     * @param jsonNode json to be validated against the schema
     * @param jsonSchema the schema to validate json node
     * @throws ValidationException if validation fails
     */
    private void validate(JsonNode jsonNode, JsonSchema jsonSchema) {
        Set<ValidationMessage> validationMessages = jsonSchema.validate(jsonNode);
        if (!validationMessages.isEmpty()) {
            String message;
            try {
                message = mapper.writeValueAsString(new ValidationErrors(validationMessages));
            } catch (JsonProcessingException e) {
                message = validationMessages.stream().map(ValidationMessage::getMessage).collect(Collectors.joining(", "));
            }
            throw new ValidationException(message);
        }
    }

    private static class ValidationErrors {

        private final Set<ValidationMessage> validationErrors;

        public ValidationErrors(Set<ValidationMessage> validationErrors) {
            this.validationErrors = validationErrors;
        }

        public Set<ValidationMessage> getValidationErrors() {
            return validationErrors;
        }
    }
}
