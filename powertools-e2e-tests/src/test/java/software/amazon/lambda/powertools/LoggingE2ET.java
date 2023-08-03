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
import static software.amazon.lambda.powertools.testutils.logging.InvocationLogs.Level.INFO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;

public class LoggingE2ET {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(LoggingE2ET.class.getSimpleName())
                .pathToFunction("logging")
                .environmentVariables(
                        Stream.of(new String[][] {
                                        {"POWERTOOLS_LOG_LEVEL", "INFO"},
                                        {"POWERTOOLS_SERVICE_NAME", LoggingE2ET.class.getSimpleName()}
                                })
                                .collect(Collectors.toMap(data -> data[0], data -> data[1])))
                .build();
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
    public void test_logInfoWithAdditionalKeys() throws JsonProcessingException {
        // GIVEN
        String orderId = UUID.randomUUID().toString();
        String event = "{\"message\":\"New Order\", \"keys\":{\"orderId\":\"" + orderId + "\"}}";

        // WHEN
        InvocationResult invocationResult1 = invokeFunction(functionName, event);
        InvocationResult invocationResult2 = invokeFunction(functionName, event);

        // THEN
        String[] functionLogs = invocationResult1.getLogs().getFunctionLogs(INFO);
        assertThat(functionLogs).hasSize(1);

        JsonNode jsonNode = objectMapper.readTree(functionLogs[0]);
        assertThat(jsonNode.get("message").asText()).isEqualTo("New Order");
        assertThat(jsonNode.get("orderId").asText()).isEqualTo(orderId);
        assertThat(jsonNode.get("coldStart").asBoolean()).isTrue();
        assertThat(jsonNode.get("function_request_id").asText()).isEqualTo(invocationResult1.getRequestId());

        // second call should not be cold start
        functionLogs = invocationResult2.getLogs().getFunctionLogs(INFO);
        assertThat(functionLogs).hasSize(1);
        jsonNode = objectMapper.readTree(functionLogs[0]);
        assertThat(jsonNode.get("coldStart").asBoolean()).isFalse();
    }
}
