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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import software.amazon.lambda.powertools.testutils.AppConfig;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.lambda.InvocationResult;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParametersE2ET {
    private final AppConfig appConfig;
    private Infrastructure infrastructure;
    private String functionName;

    public ParametersE2ET() {
        String appName = UUID.randomUUID().toString();
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        appConfig = new AppConfig(appName, "e2etest", params);
    }

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public void setup() {
        infrastructure = Infrastructure.builder()
                .testName(ParametersE2ET.class.getSimpleName())
                .pathToFunction("parameters")
                .appConfig(appConfig)
                .environmentVariables(
                        Stream.of(new String[][] {
                                        {"POWERTOOLS_LOG_LEVEL", "INFO"},
                                        {"POWERTOOLS_SERVICE_NAME", ParametersE2ET.class.getSimpleName()}
                                })
                                .collect(Collectors.toMap(data -> data[0], data -> data[1])))
                .build();
        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
    }

    @AfterAll
    public void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @Test
    public void test_getAppConfigValue() {
        for (Map.Entry<String, String> configKey : appConfig.getConfigurationValues().entrySet()) {

            // Arrange
            String event1 = "{" +
                    "\"app\":  \"" + appConfig.getApplication() + "\", " +
                    "\"environment\": \"" + appConfig.getEnvironment() + "\", " +
                    "\"key\": \"" + configKey.getKey() + "\"" +
                    "}";

            // Act
            InvocationResult invocationResult = invokeFunction(functionName, event1);

            // Assert
            assertThat(invocationResult.getResult()).isEqualTo("\"" + configKey.getValue() + "\"");
        }
    }

}
