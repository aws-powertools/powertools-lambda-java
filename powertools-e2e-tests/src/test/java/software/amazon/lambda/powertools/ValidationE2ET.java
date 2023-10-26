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

package software.amazon.lambda.powertools;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.testutils.Infrastructure.FUNCTION_NAME_OUTPUT;
import static software.amazon.lambda.powertools.testutils.lambda.LambdaInvoker.invokeFunction;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;

class ValidationE2ET {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static Infrastructure infrastructure;
	private static String functionName;

	@BeforeAll
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	public static void setup() {
		infrastructure = Infrastructure.builder().testName(ValidationE2ET.class.getSimpleName())
				.pathToFunction("validation").build();
		Map<String, String> outputs = infrastructure.deploy();
		functionName = outputs.get(FUNCTION_NAME_OUTPUT);
	}

	@AfterAll
	public static void tearDown() {
		if (infrastructure != null) {
			infrastructure.destroy();
		}
	}

	@ParameterizedTest
	@Event(value = "/validation/valid_api_gw_in_out_event.json", type = APIGatewayProxyRequestEvent.class)
	void test_validInboundApiGWEvent(APIGatewayProxyRequestEvent validEvent) throws IOException {
		// WHEN
		InvocationResult invocationResult = invokeFunction(functionName, objectMapper.writeValueAsString(validEvent));

		// THEN
		// invocation should pass validation and return 200
		JsonNode validJsonNode = objectMapper.readTree(invocationResult.getResult());
		assertThat(validJsonNode.get("statusCode").asInt()).isEqualTo(200);
		assertThat(validJsonNode.get("body").asText()).isEqualTo("{\"price\": 150}");
	}
	
	@ParameterizedTest
	@Event(value = "/validation/invalid_api_gw_in_event.json", type = APIGatewayProxyRequestEvent.class)
	void test_invalidInboundApiGWEvent(APIGatewayProxyRequestEvent validEvent) throws IOException {
		// WHEN
		InvocationResult invocationResult = invokeFunction(functionName, objectMapper.writeValueAsString(validEvent));

		// THEN
		// invocation should fail inbound validation and return 400
		JsonNode validJsonNode = objectMapper.readTree(invocationResult.getResult());
		assertThat(validJsonNode.get("statusCode").asInt()).isEqualTo(400);
		assertThat(validJsonNode.get("body").asText()).contains("$.price: is missing but it is required");
	}
	
	@ParameterizedTest
	@Event(value = "/validation/invalid_api_gw_out_event.json", type = APIGatewayProxyRequestEvent.class)
	void test_invalidOutboundApiGWEvent(APIGatewayProxyRequestEvent validEvent) throws IOException {
		// WHEN
		InvocationResult invocationResult = invokeFunction(functionName, objectMapper.writeValueAsString(validEvent));

		// THEN
		// invocation should fail outbound validation and return 400
		JsonNode validJsonNode = objectMapper.readTree(invocationResult.getResult());
		assertThat(validJsonNode.get("statusCode").asInt()).isEqualTo(400);
		assertThat(validJsonNode.get("body").asText()).contains("$.price: must have an exclusive maximum value of 1000");
	}
}
