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

import org.apache.kafka.common.utils.ByteUtils;
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
        int size = ByteUtils.readVarint(buffer);

        // Only if the size is greater than zero, continue reading varInt.
        // https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                ByteUtils.readVarint(buffer);
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
}
