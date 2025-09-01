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
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ActiveMQEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsFirehoseInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsStreamsInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.RabbitMQEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import software.amazon.lambda.powertools.common.internal.ClassPreLoader;
import software.amazon.lambda.powertools.utilities.jmespath.Base64Function;
import software.amazon.lambda.powertools.utilities.jmespath.Base64GZipFunction;
import software.amazon.lambda.powertools.utilities.jmespath.JsonFunction;

public final class JsonConfig implements Resource {

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

    // Static block to ensure CRaC registration happens at class loading time
    static {
        Core.getGlobalContext().register(get());
    }

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

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        // Preload classes first to ensure this always runs
        ClassPreLoader.preloadClasses();
        
        // Initialize key components
        ObjectMapper mapper = getObjectMapper();
        getJmesPath();
        
        // Prime common AWS Lambda event types with realistic events
        primeEventType(mapper, APIGatewayProxyRequestEvent.class, 
            "{\"httpMethod\":\"GET\",\"path\":\"/test\",\"headers\":{\"Content-Type\":\"application/json\"},\"requestContext\":{\"accountId\":\"123456789012\"}}");
        primeEventType(mapper, APIGatewayV2HTTPEvent.class, 
            "{\"version\":\"2.0\",\"routeKey\":\"GET /test\",\"requestContext\":{\"http\":{\"method\":\"GET\"},\"accountId\":\"123456789012\"}}");
        primeEventType(mapper, SQSEvent.class, 
            "{\"Records\":[{\"messageId\":\"test-id\",\"body\":\"test message\",\"eventSource\":\"aws:sqs\"}]}");
        primeEventType(mapper, SNSEvent.class, 
            "{\"Records\":[{\"Sns\":{\"Message\":\"test message\",\"TopicArn\":\"arn:aws:sns:us-east-1:123456789012:test\"}}]}");
        primeEventType(mapper, KinesisEvent.class, 
            "{\"Records\":[{\"kinesis\":{\"data\":\"dGVzdA==\",\"partitionKey\":\"test\"},\"eventSource\":\"aws:kinesis\"}]}");
        primeEventType(mapper, ScheduledEvent.class, 
            "{\"source\":\"aws.events\",\"detail-type\":\"Scheduled Event\",\"detail\":{}}");
        
        // Warm up JMESPath function registry
        getJmesPath().compile("@").search(mapper.readTree("{\"test\":\"value\"}"));
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // No action needed after restore
    }

    private void primeEventType(ObjectMapper mapper, Class<?> eventClass, String sampleJson) throws Exception {
        // Deserialize sample JSON to the event class
        Object event = mapper.readValue(sampleJson, eventClass);
        // Serialize back to JSON to warm up both directions
        mapper.writeValueAsString(event);
    }

    private static class ConfigHolder {
        private static final JsonConfig instance = new JsonConfig();
    }
}
