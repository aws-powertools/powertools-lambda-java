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

package software.amazon.lambda.powertools.utilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.function.Supplier;
import software.amazon.lambda.powertools.utilities.jmespath.Base64Function;
import software.amazon.lambda.powertools.utilities.jmespath.Base64GZipFunction;
import software.amazon.lambda.powertools.utilities.jmespath.JsonFunction;

public final class JsonConfig {

    private static final Supplier<ObjectMapper> objectMapperSupplier = () -> JsonMapper.builder()
            // Don't throw an exception when json has extra fields you are not serializing on.
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Ignore null values when writing json.
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            // Write times as a String instead of a Long so its human-readable.
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Sort fields in alphabetical order
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .build();

    private static final ThreadLocal<ObjectMapper> om = ThreadLocal.withInitial(objectMapperSupplier);

    private final FunctionRegistry defaultFunctions = FunctionRegistry.defaultRegistry();

    private final FunctionRegistry customFunctions = defaultFunctions.extend(
            new Base64Function(),
            new Base64GZipFunction(),
            new JsonFunction()
    );

    private final RuntimeConfiguration configuration = new RuntimeConfiguration.Builder()
            .withSilentTypeErrors(true)
            .withFunctionRegistry(customFunctions)
            .build();

    private JmesPath<JsonNode> jmesPath = new JacksonRuntime(configuration, getObjectMapper());

    private JsonConfig() {
    }

    public static JsonConfig get() {
        return ConfigHolder.instance;
    }

    /**
     * Return an Object Mapper. Use this to customize (de)serialization config.
     *
     * @return the {@link ObjectMapper} to serialize / deserialize JSON
     */
    public ObjectMapper getObjectMapper() {
        return om.get();
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
     * Add a custom {@link io.burt.jmespath.function.Function} to JMESPath
     * {@link Base64Function} and {@link Base64GZipFunction} are already built-in.
     *
     * @param function the function to add
     * @param <T>      Must extends {@link BaseFunction}
     */
    public <T extends BaseFunction> void addFunction(T function) {
        FunctionRegistry functionRegistryWithExtendedFunctions = configuration.functionRegistry().extend(function);

        RuntimeConfiguration updatedConfig = new RuntimeConfiguration.Builder()
                .withFunctionRegistry(functionRegistryWithExtendedFunctions)
                .build();

        jmesPath = new JacksonRuntime(updatedConfig, getObjectMapper());
    }

    private static class ConfigHolder {
        private static final JsonConfig instance = new JsonConfig();
    }
}
