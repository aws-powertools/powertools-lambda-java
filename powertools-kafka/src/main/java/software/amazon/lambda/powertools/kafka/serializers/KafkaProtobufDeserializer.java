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
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Deserializer for Kafka records using Protocol Buffers format.
 */
public class KafkaProtobufDeserializer extends AbstractKafkaDeserializer {

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T deserializeObject(byte[] data, Class<T> type) throws IOException {
        // If no Protobuf generated class is passed we cannot deserialize using Protobuf
        if (Message.class.isAssignableFrom(type)) {
            try {
                // Get the parser from the generated Protobuf class
                Parser<Message> parser = (Parser<Message>) type.getMethod("parser").invoke(null);
                Message message = parser.parseFrom(data);
                return type.cast(message);
            } catch (Exception e) {
                throw new IOException("Failed to deserialize Protobuf data.", e);
            }
        } else {
            throw new IOException("Unsupported type for Protobuf deserialization: " + type.getName() + ". "
                    + "Protobuf deserialization requires a type of com.google.protobuf.Message. "
                    + "Consider using an alternative Deserializer.");
        }
    }
}
