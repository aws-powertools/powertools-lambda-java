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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;
import software.amazon.lambda.powertools.utilities.JsonConfig;

class LambdaMetadataE2ET {

    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    static void setup() {
        infrastructure = Infrastructure.builder()
                .testName(LambdaMetadataE2ET.class.getSimpleName())
                .pathToFunction("lambda-metadata")
                .build();
        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
    }

    @AfterAll
    static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @Test
    void test_getMetadata() throws Exception {
        // WHEN
        InvocationResult invocationResult = invokeFunction(functionName, "{}");

        // THEN
        assertThat(invocationResult.getFunctionError())
                .describedAs("Lambda function failed: %s", invocationResult.getResult())
                .isNull();

        JsonNode response = JsonConfig.get().getObjectMapper().readTree(invocationResult.getResult());
        String availabilityZoneId = response.get("availabilityZoneId").asText();
        assertThat(availabilityZoneId).isNotNull().matches("[a-z]{3,4}\\d+-az\\d+");
    }

    @Test
    void test_metadataCaching() throws Exception {
        // WHEN - invoke twice (both invocations hit the same sandbox)
        InvocationResult firstResult = invokeFunction(functionName, "{}");
        InvocationResult secondResult = invokeFunction(functionName, "{}");

        // THEN - both should return the same AZ ID (cached within sandbox)
        assertThat(firstResult.getFunctionError())
                .describedAs("Lambda function failed on first invocation: %s", firstResult.getResult())
                .isNull();
        assertThat(secondResult.getFunctionError())
                .describedAs("Lambda function failed on second invocation: %s", secondResult.getResult())
                .isNull();

        JsonNode firstResponse = JsonConfig.get().getObjectMapper().readTree(firstResult.getResult());
        JsonNode secondResponse = JsonConfig.get().getObjectMapper().readTree(secondResult.getResult());

        String firstAzId = firstResponse.get("availabilityZoneId").asText();
        String secondAzId = secondResponse.get("availabilityZoneId").asText();

        assertThat(firstAzId).isNotNull().matches("[a-z]{3,4}\\d+-az\\d+").isEqualTo(secondAzId);
    }
}
