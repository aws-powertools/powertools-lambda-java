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

package software.amazon.lambda.powertools.testutils;

import java.util.Map;

/**
 * Defines configuration used to setup an AppConfig
 * deployment when the infrastructure is rolled out.
 * <p>
 * All fields are non-nullable.
 */
public class AppConfig {
    private String application;
    private String environment;
    private Map<String, String> configurationValues;

    public AppConfig(String application, String environment, Map<String, String> configurationValues) {
        this.application = application;
        this.environment = environment;
        this.configurationValues = configurationValues;
    }

    public String getApplication() {
        return application;
    }

    public String getEnvironment() {
        return environment;
    }

    public Map<String, String> getConfigurationValues() {
        return configurationValues;
    }
}
