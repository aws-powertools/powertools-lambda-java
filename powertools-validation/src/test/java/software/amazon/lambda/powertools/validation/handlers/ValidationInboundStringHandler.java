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
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import software.amazon.lambda.powertools.validation.Validator;


public class ValidationInboundStringHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final String schema = "{" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\"," +
            "  \"title\": \"Product\"," +
            "  \"description\": \"A product from the catalog\"," +
            "  \"type\": \"object\"," +
            "  \"properties\": {" +
            "    \"id\": {" +
            "      \"description\": \"The unique identifier for a product\"," +
            "      \"type\": \"integer\"" +
            "    }," +
            "    \"name\": {" +
            "      \"description\": \"Name of the product\"," +
            "      \"type\": \"string\"" +
            "    }," +
            "    \"price\": {" +
            "      \"type\": \"number\"," +
            "      \"minimum\": 0," +
            "      \"exclusiveMinimum\": true" +
            "    }" +
            "  }," +
            "  \"required\": [\"id\", \"name\", \"price\"]" +
            "}";
    
    @Override
    @Validator(inboundSchema = schema)
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        return null;
    }
}
