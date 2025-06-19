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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Deserializer for Kafka records using Protocol Buffers format.
 * Supports both standard protobuf serialization and Confluent Schema Registry serialization using messages indices.
 * 
 * For Confluent-serialized data, assumes the magic byte and schema ID have already been stripped
 * by the Kafka ESM, leaving only the message index (if present) and protobuf data.
 * 
 * @see {@link https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format}
 */
public class KafkaProtobufDeserializer extends AbstractKafkaDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProtobufDeserializer.class);

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T deserializeObject(byte[] data, Class<T> type) throws IOException {
        // If no Protobuf generated class is passed we cannot deserialize using Protobuf
        if (Message.class.isAssignableFrom(type)) {
            try {
                // Get the parser from the generated Protobuf class
                Parser<Message> parser = (Parser<Message>) type.getMethod("parser").invoke(null);

                // Try to deserialize the data, handling potential Confluent message indices
                Message message = deserializeWithMessageIndexHandling(data, parser);
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

    private Message deserializeWithMessageIndexHandling(byte[] data, Parser<Message> parser) throws IOException {
        try {
            LOGGER.debug("Attempting to deserialize as standard protobuf data");
            return parser.parseFrom(data);
        } catch (Exception e) {
            LOGGER.debug("Standard protobuf parsing failed, attempting Confluent message-index handling");
            return deserializeWithMessageIndex(data, parser);
        }
    }

    private Message deserializeWithMessageIndex(byte[] data, Parser<Message> parser) throws IOException {
        CodedInputStream codedInputStream = CodedInputStream.newInstance(data);

        try {
            // https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format
            // Read the first varint - this could be:
            // 1. A single 0 (simple case - first message type)
            // 2. The length of the message index array (complex case)
            int firstValue = codedInputStream.readUInt32();

            if (firstValue == 0) {
                // Simple case: Single 0 byte means first message type
                LOGGER.debug("Found simple message-index case (single 0), parsing remaining data as protobuf");
                return parser.parseFrom(codedInputStream);
            } else {
                // Complex case: firstValue is the length of the message index array
                LOGGER.debug("Found complex message-index case with array length: {}, skipping {} message index values",
                        firstValue, firstValue);
                for (int i = 0; i < firstValue; i++) {
                    codedInputStream.readUInt32(); // Skip each message index value
                }
                // Now the remaining data should be the actual protobuf message
                LOGGER.debug("Finished skipping message indexes, parsing remaining data as protobuf");
                return parser.parseFrom(codedInputStream);
            }

        } catch (Exception e) {
            throw new IOException("Failed to parse protobuf data with or without message index", e);
        }
    }
}
