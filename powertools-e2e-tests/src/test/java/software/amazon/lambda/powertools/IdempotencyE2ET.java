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
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;

public class IdempotencyE2ET {
    private static Infrastructure infrastructure;
    private static String functionName;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        String random = UUID.randomUUID().toString().substring(0, 6);
        infrastructure = Infrastructure.builder()
                .testName(IdempotencyE2ET.class.getSimpleName())
                .pathToFunction("idempotency")
                .idempotencyTable("idempo" + random)
                .environmentVariables(Collections.singletonMap("IDEMPOTENCY_TABLE", "idempo" + random))
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
    public void test_ttlNotExpired_sameResult_ttlExpired_differentResult() throws InterruptedException {
        // GIVEN
        String event = "{\"message\":\"TTL 10sec\"}";

        // WHEN
        // First invocation
        InvocationResult result1 = invokeFunction(functionName, event);

        // Second invocation (should get same result)
        InvocationResult result2 = invokeFunction(functionName, event);

        Thread.sleep(12000);

        // Third invocation (should get different result)
        InvocationResult result3 = invokeFunction(functionName, event);

        // THEN
        Assertions.assertThat(result1.getResult()).contains(Year.now().toString());
        Assertions.assertThat(result2.getResult()).isEqualTo(result1.getResult());
        Assertions.assertThat(result3.getResult()).isNotEqualTo(result2.getResult());
    }
}
