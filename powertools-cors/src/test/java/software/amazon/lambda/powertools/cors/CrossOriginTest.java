/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.cors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.lambda.powertools.cors.handlers.CorsFunction;
import software.amazon.lambda.powertools.cors.handlers.CorsNotOnHandlerFunction;
import software.amazon.lambda.powertools.cors.handlers.CorsOnSqsFunction;
import software.amazon.lambda.powertools.cors.handlers.DefaultCorsFunction;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.cors.Constants.*;

public class CrossOriginTest {
    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void functionWithCustomAndDefaultCorsConfig_shouldHaveCorsHeaders() {
        CorsFunction function = new CorsFunction();
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = function.handleRequest(event, context);

        Map<String, String> headers = response.getHeaders();
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_ORIGIN, "origin.com");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_METHODS, "POST, OPTIONS");
        assertThat(headers).containsEntry(ACCESS_CONTROL_MAX_AGE, String.valueOf(DEFAULT_ACCESS_CONTROL_MAX_AGE));
        assertThat(headers).containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS);
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @SetEnvironmentVariable.SetEnvironmentVariables(value = {
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_HEADERS, value = "Content-Type, X-Amz-Date"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_ORIGIN, value = "example.com, origin.com"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_METHODS, value = "OPTIONS, POST"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_EXPOSE_HEADERS, value = "Content-Type, X-Amz-Date"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_ALLOW_CREDENTIALS, value = "true"),
            @SetEnvironmentVariable(key = ENV_ACCESS_CONTROL_MAX_AGE, value = "42"),
    })
    public void functionWithCustomCorsEnvVars_shouldHaveCorsHeaders() {
        DefaultCorsFunction function = new DefaultCorsFunction();
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = function.handleRequest(event, context);

        Map<String, String> headers = response.getHeaders();
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_ORIGIN, "origin.com");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, X-Amz-Date");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, POST");
        assertThat(headers).containsEntry(ACCESS_CONTROL_MAX_AGE, "42");
        assertThat(headers).containsEntry(ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Type, X-Amz-Date");
        assertThat(headers).containsEntry(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    public void functionWithCorsOnSqsEvent() {
        CorsOnSqsFunction function = new CorsOnSqsFunction();
        SQSEvent sqsEvent = EventLoader.loadSQSEvent("sqs_event.json");
        SQSBatchResponse response = function.handleRequest(sqsEvent, context);
        assertThat(response).isNotNull();
    }

    @Test
    public void functionWithCorsNotOnHandler_shouldNotContainCorsHeaders() {
        CorsNotOnHandlerFunction function = new CorsNotOnHandlerFunction();
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        APIGatewayProxyResponseEvent response = function.handleRequest(event, context);
        assertThat(response.getHeaders()).isNull();
    }

}
