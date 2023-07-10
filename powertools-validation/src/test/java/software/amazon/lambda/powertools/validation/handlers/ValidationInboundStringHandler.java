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
package software.amazon.lambda.powertools.validation.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import software.amazon.lambda.powertools.validation.Validation;


public class ValidationInboundStringHandler implements RequestHandler<APIGatewayV2HTTPEvent, String> {

    private static final String schema = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n" +
            "  \"$id\": \"http://example.com/product.json\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"title\": \"Product schema\",\n" +
            "  \"description\": \"JSON schema to validate Products\",\n" +
            "  \"default\": {},\n" +
            "  \"examples\": [\n" +
            "    {\n" +
            "      \"id\": 43242,\n" +
            "      \"name\": \"FooBar XY\",\n" +
            "      \"price\": 258\n" +
            "    }\n" +
            "  ],\n" +
            "  \"required\": [\n" +
            "    \"id\",\n" +
            "    \"name\",\n" +
            "    \"price\"\n" +
            "  ],\n" +
            "  \"properties\": {\n" +
            "    \"id\": {\n" +
            "      \"$id\": \"#/properties/id\",\n" +
            "      \"type\": \"integer\",\n" +
            "      \"title\": \"Id of the product\",\n" +
            "      \"description\": \"Unique identifier of the product\",\n" +
            "      \"default\": 0,\n" +
            "      \"examples\": [\n" +
            "        43242\n" +
            "      ]\n" +
            "    },\n" +
            "    \"name\": {\n" +
            "      \"$id\": \"#/properties/name\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"title\": \"Name of the product\",\n" +
            "      \"description\": \"Explicit name of the product\",\n" +
            "      \"minLength\": 5,\n" +
            "      \"default\": \"\",\n" +
            "      \"examples\": [\n" +
            "        \"FooBar XY\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"price\": {\n" +
            "      \"$id\": \"#/properties/price\",\n" +
            "      \"type\": \"number\",\n" +
            "      \"title\": \"Price of the product\",\n" +
            "      \"description\": \"Positive price of the product\",\n" +
            "      \"default\": 0,\n" +
            "      \"exclusiveMinimum\": 0,\n" +
            "      \"examples\": [\n" +
            "        258.99\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"additionalProperties\": true\n" +
            "}";

    @Override
    @Validation(inboundSchema = schema)
    public String handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        return "OK";
    }
}
