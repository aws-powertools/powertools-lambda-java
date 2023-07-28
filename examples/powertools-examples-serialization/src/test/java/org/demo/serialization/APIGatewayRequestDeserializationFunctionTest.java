/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
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

package org.demo.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class APIGatewayRequestDeserializationFunctionTest {

    @Mock
    private Context context;
    private APIGatewayRequestDeserializationFunction deserializationFunction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deserializationFunction = new APIGatewayRequestDeserializationFunction();
    }

    @Test
    public void shouldReturnOkStatusWithProductId() {
        String body = "{\"id\":1234, \"name\":\"product\", \"price\":42}";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody(body);

        APIGatewayProxyResponseEvent response = deserializationFunction.handleRequest(request, context);

        assertEquals(200, response.getStatusCode());
        assertEquals("Received request for productId: 1234", response.getBody());
    }
}