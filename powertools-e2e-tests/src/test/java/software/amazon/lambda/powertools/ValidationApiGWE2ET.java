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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;

class ValidationApiGWE2ET {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static Infrastructure infrastructure;
	private static String functionName;

	@BeforeAll
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	public static void setup() {
		infrastructure = Infrastructure.builder().testName(ValidationApiGWE2ET.class.getSimpleName())
				.pathToFunction("validation-apigw-event").build();
		Map<String, String> outputs = infrastructure.deploy();
		functionName = outputs.get(FUNCTION_NAME_OUTPUT);
	}

	@AfterAll
	public static void tearDown() {
		if (infrastructure != null) {
			infrastructure.destroy();
		}
	}

	@Test
	void test_validInboundApiGWEvent() throws IOException {
		InputStream inputStream = this.getClass().getResourceAsStream("/validation/valid_api_gw_in_out_event.json");
		String validEvent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

		// WHEN
		InvocationResult invocationResult = invokeFunction(functionName, validEvent);

		// THEN
		// invocation should pass validation and return 200
		JsonNode validJsonNode = objectMapper.readTree(invocationResult.getResult());
		assertThat(validJsonNode.get("statusCode").asInt()).isEqualTo(200);
		assertThat(validJsonNode.get("body").asText()).isEqualTo("{\"price\": 150}");
	}

	@Test
	void test_invalidInboundApiGWEvent() throws IOException {
		InputStream inputStream = this.getClass().getResourceAsStream("/validation/invalid_api_gw_in_event.json");
		String invalidEvent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

		// WHEN
		InvocationResult invocationResult = invokeFunction(functionName, invalidEvent);

		// THEN
		// invocation should fail inbound validation and return 400
		JsonNode validJsonNode = objectMapper.readTree(invocationResult.getResult());
		assertThat(validJsonNode.get("statusCode").asInt()).isEqualTo(400);
		assertThat(validJsonNode.get("body").asText()).contains(": required property 'price' not found");
	}

	@Test
	void test_invalidOutboundApiGWEvent() throws IOException {
		InputStream inputStream = this.getClass().getResourceAsStream("/validation/invalid_api_gw_out_event.json");
		String invalidEvent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

		// WHEN
		InvocationResult invocationResult = invokeFunction(functionName, invalidEvent);

		// THEN
		// invocation should fail outbound validation and return 400
		JsonNode validJsonNode = objectMapper.readTree(invocationResult.getResult());
		assertThat(validJsonNode.get("statusCode").asInt()).isEqualTo(400);
		assertThat(validJsonNode.get("body").asText())
				.contains("/price: must have an exclusive maximum value of 1000");
	}
}
