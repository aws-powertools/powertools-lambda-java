/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;
import software.amazon.lambda.powertools.validation.jmespath.Base64Function;
import software.amazon.lambda.powertools.validation.jmespath.Base64GZipFunction;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * Use this if you need to customize some part of the JSON Schema validation
 * (eg. specification version, Jackson ObjectMapper, or adding functions to JMESPath)
 */
public class ValidationConfig {
    private ValidationConfig() {
    }

    private static class ConfigHolder {
        private final static ValidationConfig instance = new ValidationConfig();
    }

    public static ValidationConfig get() {
        return ConfigHolder.instance;
    }

    private static final ThreadLocal<ObjectMapper> om = ThreadLocal.withInitial(() -> {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    });

    private SpecVersion.VersionFlag jsonSchemaVersion = SpecVersion.VersionFlag.V7;
    private JsonSchemaFactory factory = JsonSchemaFactory.getInstance(jsonSchemaVersion);

    private final FunctionRegistry defaultFunctions = FunctionRegistry.defaultRegistry();
    private final FunctionRegistry customFunctions = defaultFunctions.extend(
            new Base64Function(),
            new Base64GZipFunction());
    private final RuntimeConfiguration configuration = new RuntimeConfiguration.Builder()
            .withFunctionRegistry(customFunctions)
            .build();
    private JmesPath<JsonNode> jmesPath = new JacksonRuntime(configuration, getObjectMapper());

    /**
     * Set the version of the json schema specifications (default is V7)
     *
     * @param version May be V4, V6, V7 or V201909
     */
    public void setSchemaVersion(SpecVersion.VersionFlag version) {
        if (version != jsonSchemaVersion) {
            jsonSchemaVersion = version;
            factory = JsonSchemaFactory.getInstance(version);
        }
    }

    public SpecVersion.VersionFlag getSchemaVersion() {
        return jsonSchemaVersion;
    }

    /**
     * Add a custom {@link io.burt.jmespath.function.Function} to JMESPath
     * {@link Base64Function} and {@link Base64GZipFunction} are already built-in.
     *
     * @param function the function to add
     * @param <T> Must extends {@link BaseFunction}
     */
    public <T extends BaseFunction> void addFunction(T function) {
        FunctionRegistry functionRegistryWithExtendedFunctions = configuration.functionRegistry().extend(function);

        RuntimeConfiguration updatedConfig = new RuntimeConfiguration.Builder()
                .withFunctionRegistry(functionRegistryWithExtendedFunctions)
                .build();

        jmesPath = new JacksonRuntime(updatedConfig, getObjectMapper());
    }

    /**
     * Return the Json Schema Factory, used to load schemas
     *
     * @return the Json Schema Factory
     */
    public JsonSchemaFactory getFactory() {
        return factory;
    }

    /**
     * Return the JmesPath used to select sub node of Json
     *
     * @return the {@link JmesPath}
     */
    public JmesPath<JsonNode> getJmesPath() {
        return jmesPath;
    }

    /**
     * Return an Object Mapper. Use this to customize (de)serialization config.
     *
     * @return the {@link ObjectMapper} to serialize / deserialize JSON
     */
    public ObjectMapper getObjectMapper() {
        return om.get();
    }
}
