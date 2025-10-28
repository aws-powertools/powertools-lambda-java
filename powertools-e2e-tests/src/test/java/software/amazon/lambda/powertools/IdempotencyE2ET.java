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

import static software.amazon.lambda.powertools.testutils.Infrastructure.FUNCTION_NAME_OUTPUT;
import static software.amazon.lambda.powertools.testutils.lambda.LambdaInvoker.invokeFunction;

import java.time.Year;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdempotencyE2ET {
    private Infrastructure infrastructure;
    private String functionName;

    private void setupInfrastructure(String pathToFunction) {
        String random = UUID.randomUUID().toString().substring(0, 6);
        infrastructure = Infrastructure.builder()
                .testName(IdempotencyE2ET.class.getSimpleName() + "-" + pathToFunction)
                .pathToFunction(pathToFunction)
                .idempotencyTable("idempo" + random)
                .build();
        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
    }

    @AfterEach
    void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "idempotency", "idempotency-functional" })
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void test_ttlNotExpired_sameResult_ttlExpired_differentResult(String pathToFunction) throws InterruptedException {
        setupInfrastructure(pathToFunction);
        // GIVEN
        String event = "{\"message\":\"TTL 10sec\"}";

        // WHEN
        // First invocation
        InvocationResult result1 = invokeFunction(functionName, event);

        // Second invocation (should get same result)
        InvocationResult result2 = invokeFunction(functionName, event);

        // Function idempotency record expiration is set to 10 seconds
        Thread.sleep(12000);

        // Third invocation (should get different result)
        InvocationResult result3 = invokeFunction(functionName, event);

        // THEN
        Assertions.assertThat(result1.getResult()).contains(Year.now().toString());
        Assertions.assertThat(result2.getResult()).isEqualTo(result1.getResult());
        Assertions.assertThat(result3.getResult()).isNotEqualTo(result2.getResult());
    }
}
