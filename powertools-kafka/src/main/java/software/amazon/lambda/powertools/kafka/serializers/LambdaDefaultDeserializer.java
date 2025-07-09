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
 */
package software.amazon.lambda.powertools.kafka.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;

/**
 * Default deserializer for Kafka events proxying to Lambda default behavior.
 * 
 * This deserializer uses the default Jackson ObjectMapper to deserialize the event from 
 * {@link com.amazonaws.services.lambda.runtime.serialization}.
 */
public class LambdaDefaultDeserializer implements PowertoolsDeserializer {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(InputStream input, Type type) {
        // If the target type does not require conversion, simply return the value itself
        if (type.equals(InputStream.class)) {
            return (T) input;
        }

        // If the target type is String, read the input stream as a String
        if (type.equals(String.class)) {
            try {
                return (T) new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read input stream as String", e);
            }
        }

        return (T) JacksonFactory.getInstance().getSerializer(type).fromJson(input);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(String input, Type type) {
        // If the target type does not require conversion, simply return the value itself
        if (type.equals(String.class)) {
            return (T) input;
        }

        // If the target type is InputStream, read the input stream as a String
        if (type.equals(InputStream.class)) {
            return (T) input.getBytes(StandardCharsets.UTF_8);
        }

        return (T) JacksonFactory.getInstance().getSerializer(type).fromJson(input);
    }
}
