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
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Deserializer for Kafka records using Protocol Buffers format.
 * Supports both standard protobuf serialization and Confluent / Glue Schema Registry serialization using messages 
 * indices.
 * 
 * For Confluent-serialized data, assumes the magic byte and schema ID have already been stripped
 * by the Kafka ESM, leaving only the message index (if present) and protobuf data.
 * 
 * @see {@link https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format}
 */
public class KafkaProtobufDeserializer extends AbstractKafkaDeserializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProtobufDeserializer.class);
    private static final String PROTOBUF_PARSER_METHOD = "parser";
    private static final int MAX_VARINT_BYTES = 5;

    @Override
    protected <T> T deserializeObject(byte[] data, Class<T> type, SchemaRegistryType schemaRegistryType)
            throws IOException {
        // If no Protobuf generated class is passed, we cannot deserialize using Protobuf
        if (Message.class.isAssignableFrom(type)) {
            try {
                switch (schemaRegistryType) {
                    case GLUE:
                        return glueDeserializer(data, type);
                    case CONFLUENT:
                        return confluentDeserializer(data, type);
                    default:
                        return defaultDeserializer(data, type);
                }
            } catch (Exception e) {
                throw new IOException("Failed to deserialize Protobuf data.", e);
            }
        } else {
            throw new IOException("Unsupported type for Protobuf deserialization: " + type.getName() + ". "
                    + "Protobuf deserialization requires a type of com.google.protobuf.Message. "
                    + "Consider using an alternative Deserializer.");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T defaultDeserializer(byte[] data, Class<T> type) throws IOException {
        try {
            LOGGER.debug("Using default Protobuf deserializer");
            Parser<Message> parser = (Parser<Message>) type.getMethod(PROTOBUF_PARSER_METHOD).invoke(null);
            Message message = parser.parseFrom(data);
            return type.cast(message);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize Protobuf data.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T confluentDeserializer(byte[] data, Class<T> type)
            throws IOException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        LOGGER.debug("Using Confluent Deserializer");
        Parser<Message> parser = (Parser<Message>) type.getMethod(PROTOBUF_PARSER_METHOD).invoke(null);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int size = readVarint(buffer);

        // Only if the size is greater than zero, continue reading varInt.
        // https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                readVarint(buffer);
            }
        }
        Message message = parser.parseFrom(buffer);
        return type.cast(message);
    }

    @SuppressWarnings("unchecked")
    private <T> T glueDeserializer(byte[] data, Class<T> type)
            throws IOException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        LOGGER.debug("Using Glue Deserializer");
        CodedInputStream codedInputStream = CodedInputStream.newInstance(data);
        Parser<Message> parser = (Parser<Message>) type.getMethod(PROTOBUF_PARSER_METHOD).invoke(null);

        // Seek one byte forward. Based on Glue Proto deserializer implementation
        codedInputStream.readUInt32();

        Message message = parser.parseFrom(codedInputStream);
        return type.cast(message);
    }

    /**
     * Reads a zig-zag encoded variable-length integer as used by the Confluent Protobuf wire format for message
     * indexes. Behaves identically to Kafka's {@code ByteUtils.readVarint(ByteBuffer)}, which moved to an internal
     * package in kafka-clients 4.4.0 (KAFKA-20297) and is no longer part of the public API.
     *
     * @param buffer the buffer to read from
     * @return the decoded integer
     * @throws IllegalArgumentException if the varint does not terminate within 5 bytes
     */
    static int readVarint(ByteBuffer buffer) {
        int value = readUnsignedVarint(buffer);
        return (value >>> 1) ^ -(value & 1);
    }

    private static int readUnsignedVarint(ByteBuffer buffer) {
        int result = 0;
        for (int shift = 0; shift < MAX_VARINT_BYTES * 7; shift += 7) {
            byte b = buffer.get();
            result |= (b & 0x7F) << shift;
            if (b >= 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("Varint is too long, the most significant bit in the 5th byte is set, "
                + "converted value: " + Integer.toHexString(result));
    }
}
