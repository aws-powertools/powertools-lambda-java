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

package software.amazon.lambda.powertools.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.function.BaseFunction;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import software.amazon.lambda.powertools.common.internal.ClassPreLoader;
import software.amazon.lambda.powertools.utilities.JsonConfig;
import software.amazon.lambda.powertools.utilities.jmespath.Base64Function;
import software.amazon.lambda.powertools.utilities.jmespath.Base64GZipFunction;

/**
 * Use this if you need to customize some part of the JSON Schema validation
 * (eg. specification version, Jackson ObjectMapper, or adding functions to JMESPath).
 * <p>
 * For everything but the validation features (factory, schemaVersion), {@link ValidationConfig}
 * is just a wrapper of {@link JsonConfig}.
 */
public class ValidationConfig implements Resource {
    private SpecVersion.VersionFlag jsonSchemaVersion = SpecVersion.VersionFlag.V7;
    private JsonSchemaFactory factory = JsonSchemaFactory.getInstance(jsonSchemaVersion);

    // Static block to ensure CRaC registration happens at class loading time
    static {
        Core.getGlobalContext().register(get());
    }

    private ValidationConfig() {
    }

    public static ValidationConfig get() {
        return ConfigHolder.instance;
    }

    public SpecVersion.VersionFlag getSchemaVersion() {
        return jsonSchemaVersion;
    }

    /**
     * Set the version of the json schema specifications to use if $schema is not
     * explicitly specified within the schema (default is V7). If $schema is
     * explicitly specified within the schema is explicitly specified within the
     * schema, the validator will use the specified dialect.
     *
     * @param version May be V4, V6, V7, V201909 or V202012
     * @see <a href=
     *      "https://json-schema.org/understanding-json-schema/reference/schema#declaring-a-dialect">Declaring
     *      a Dialect</a>
     */
    public void setSchemaVersion(SpecVersion.VersionFlag version) {
        if (version != jsonSchemaVersion) {
            jsonSchemaVersion = version;
            factory = JsonSchemaFactory.getInstance(version);
        }
    }

    /**
     * Add a custom {@link io.burt.jmespath.function.Function} to JMESPath
     * {@link Base64Function} and {@link Base64GZipFunction} are already built-in.
     *
     * @param function the function to add
     * @param <T>      Must extend {@link BaseFunction}
     */
    public <T extends BaseFunction> void addFunction(T function) {
        JsonConfig.get().addFunction(function);
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
        return JsonConfig.get().getJmesPath();
    }

    /**
     * Return an Object Mapper. Use this to customize (de)serialization config.
     *
     * @return the {@link ObjectMapper} to serialize / deserialize JSON
     */
    public ObjectMapper getObjectMapper() {
        return JsonConfig.get().getObjectMapper();
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        // Initialize key components
        getObjectMapper();
        getJmesPath();
        getFactory();

        // Dummy validation
        String sampleSchema = "{\"type\":\"object\"}";
        JsonSchema schema = ValidationUtils.getJsonSchema(sampleSchema);
        ValidationUtils.validate("{\"test\":\"dummy\"}", schema);

        ClassPreLoader.preloadClasses();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // No action needed after restore
    }

    private static class ConfigHolder {
        private static final ValidationConfig instance = new ValidationConfig();
    }
}
