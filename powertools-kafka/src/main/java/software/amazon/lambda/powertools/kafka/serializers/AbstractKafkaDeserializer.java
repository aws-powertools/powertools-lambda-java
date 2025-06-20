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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;

import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract base class for Kafka deserializers that implements common functionality.
 */
abstract class AbstractKafkaDeserializer implements PowertoolsDeserializer {
    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Integer GLUE_SCHEMA_ID_LENGTH = 36;

    public enum SchemaRegistryType {
        CONFLUENT, GLUE, NONE
    }

    /**
     * Deserialize JSON from InputStream into ConsumerRecords
     *
     * @param input InputStream containing JSON data
     * @param type Type representing ConsumerRecords<K, V>
     * @param <T> The type to deserialize to
     * @return Deserialized ConsumerRecords object
     * @throws IllegalArgumentException if type is not ConsumerRecords
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(InputStream input, Type type) {
        if (!isConsumerRecordsType(type)) {
            throw new IllegalArgumentException("Type must be ConsumerRecords<K, V> when using this deserializer");
        }

        try {
            // Parse the KafkaEvent from the input stream
            KafkaEvent kafkaEvent = objectMapper.readValue(input, KafkaEvent.class);

            // Extract the key and value types from the ConsumerRecords<K, V> type
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Class<?> keyType = (Class<?>) typeArguments[0];
            Class<?> valueType = (Class<?>) typeArguments[1];

            // Convert KafkaEvent to ConsumerRecords
            return (T) convertToConsumerRecords(kafkaEvent, keyType, valueType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize Lambda handler input to ConsumerRecords", e);
        }
    }

    /**
     * Deserialize JSON from String into ConsumerRecords
     *
     * @param input String containing JSON data
     * @param type Type representing ConsumerRecords<K, V>
     * @param <T> The type to deserialize to
     * @return Deserialized ConsumerRecords object
     * @throws IllegalArgumentException if type is not ConsumerRecords
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(String input, Type type) {
        if (!isConsumerRecordsType(type)) {
            throw new IllegalArgumentException("Type must be ConsumerRecords<K, V> when using this deserializer");
        }

        try {
            // Parse the KafkaEvent from the input string
            KafkaEvent kafkaEvent = objectMapper.readValue(input, KafkaEvent.class);

            // Extract the key and value types from the ConsumerRecords<K, V> type
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Class<?> keyType = (Class<?>) typeArguments[0];
            Class<?> valueType = (Class<?>) typeArguments[1];

            // Convert KafkaEvent to ConsumerRecords
            return (T) convertToConsumerRecords(kafkaEvent, keyType, valueType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize Lambda handler input to ConsumerRecords", e);
        }
    }

    private boolean isConsumerRecordsType(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        return parameterizedType.getRawType().equals(ConsumerRecords.class);
    }

    private <K, V> ConsumerRecords<K, V> convertToConsumerRecords(KafkaEvent kafkaEvent, Class<K> keyType,
            Class<V> valueType) {
        // Validate that this is actually a Kafka event by checking for required properties
        if (kafkaEvent == null || kafkaEvent.getEventSource() == null) {
            throw new RuntimeException(
                    "Failed to deserialize Lambda handler input to ConsumerRecords: Input is not a valid Kafka event.");
        }

        if (kafkaEvent.getRecords() == null) {
            return ConsumerRecords.empty();
        }

        Map<TopicPartition, List<ConsumerRecord<K, V>>> recordsMap = new HashMap<>();

        for (Map.Entry<String, List<KafkaEvent.KafkaEventRecord>> entry : kafkaEvent.getRecords().entrySet()) {
            String topic = entry.getKey();

            for (KafkaEvent.KafkaEventRecord eventRecord : entry.getValue()) {
                ConsumerRecord<K, V> consumerRecord = convertToConsumerRecord(topic, eventRecord, keyType, valueType);

                TopicPartition topicPartition = new TopicPartition(topic, eventRecord.getPartition());
                recordsMap.computeIfAbsent(topicPartition, k -> new ArrayList<>()).add(consumerRecord);
            }
        }

        return createConsumerRecords(recordsMap);
    }

    /**
     * Creates ConsumerRecords with compatibility for both Kafka 3.x.x and 4.x.x.
     * 
     * @param <K> Key type
     * @param <V> Value type
     * @param records Map of records by topic partition
     * @return ConsumerRecords instance
     */
    protected <K, V> ConsumerRecords<K, V> createConsumerRecords(
            Map<TopicPartition, List<ConsumerRecord<K, V>>> records) {
        try {
            // Try to use the Kafka 4.x.x constructor with nextOffsets parameter
            return new ConsumerRecords<>(records, Map.of());
        } catch (NoSuchMethodError e) {
            // Fall back to Kafka 3.x.x constructor if 4.x.x is not available
            return new ConsumerRecords<>(records);
        }
    }

    private <K, V> ConsumerRecord<K, V> convertToConsumerRecord(
            String topic,
            KafkaEvent.KafkaEventRecord eventRecord,
            Class<K> keyType,
            Class<V> valueType) {

        K key = deserializeField(eventRecord.getKey(), keyType, "key", extractSchemaRegistryType(eventRecord));
        V value = deserializeField(eventRecord.getValue(), valueType, "value", extractSchemaRegistryType(eventRecord));
        Headers headers = extractHeaders(eventRecord);

        return new ConsumerRecord<>(
                topic,
                eventRecord.getPartition(),
                eventRecord.getOffset(),
                eventRecord.getTimestamp(),
                TimestampType.valueOf(eventRecord.getTimestampType()),
                // We set these to NULL_SIZE since they are not relevant in the Lambda environment due to ESM
                // pre-processing.
                ConsumerRecord.NULL_SIZE,
                ConsumerRecord.NULL_SIZE,
                key,
                value,
                headers,
                Optional.empty());
    }

    private <T> T deserializeField(String encodedData, Class<T> type, String fieldName,
            SchemaRegistryType schemaRegistryType) {
        if (encodedData == null) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
            return deserialize(decodedBytes, type, schemaRegistryType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Kafka record " + fieldName + ".", e);
        }
    }

    private Headers extractHeaders(KafkaEvent.KafkaEventRecord eventRecord) {
        Headers headers = new RecordHeaders();
        if (eventRecord.getHeaders() != null) {
            for (Map<String, byte[]> headerMap : eventRecord.getHeaders()) {
                for (Map.Entry<String, byte[]> header : headerMap.entrySet()) {
                    if (header.getValue() != null) {
                        headers.add(header.getKey(), header.getValue());
                    }
                }
            }
        }

        return headers;
    }

    private String extractKeySchemaId(KafkaEvent.KafkaEventRecord eventRecord) {
        if (eventRecord.getKeySchemaMetadata() != null) {
            return eventRecord.getKeySchemaMetadata().getSchemaId();
        }
        return null;
    }

    private String extractValueSchemaId(KafkaEvent.KafkaEventRecord eventRecord) {
        if (eventRecord.getValueSchemaMetadata() != null) {
            return eventRecord.getValueSchemaMetadata().getSchemaId();
        }
        return null;
    }

    private SchemaRegistryType extractSchemaRegistryType(KafkaEvent.KafkaEventRecord eventRecord) {
        // This method is used for both key and value, so we try to extract the schema id from both fields
        String schemaId = extractValueSchemaId(eventRecord);
        if (schemaId == null) {
            schemaId = extractKeySchemaId(eventRecord);
        }

        if (schemaId == null) {
            return SchemaRegistryType.NONE;
        }

        return schemaId.length() == GLUE_SCHEMA_ID_LENGTH ? SchemaRegistryType.GLUE : SchemaRegistryType.CONFLUENT;
    }

    /**
     * Template method to be implemented by subclasses for specific deserialization logic
     * for complex types (non-primitives) and for specific Schema Registry type.
     *
     * @param <T> The type to deserialize to
     * @param data The byte array to deserialize coming from the base64 decoded Kafka field
     * @param type The class type to deserialize to
     * @param schemaRegistryType Schema Registry type
     * @return The deserialized object
     * @throws IOException If deserialization fails
     */
    protected abstract <T> T deserializeObject(byte[] data, Class<T> type, SchemaRegistryType schemaRegistryType)
            throws IOException;

    /**
     * Main deserialize method that handles primitive types and delegates to subclasses for complex types and
     * for specific Schema Registry type.
     *
     * @param <T> The type to deserialize to
     * @param data The byte array to deserialize
     * @param type The class type to deserialize to
     * @param schemaRegistryType Schema Registry type
     * @return The deserialized object
     * @throws IOException If deserialization fails
     */
    private <T> T deserialize(byte[] data, Class<T> type, SchemaRegistryType schemaRegistryType) throws IOException {
        // First try to deserialize as a primitive type
        T result = deserializePrimitive(data, type);
        if (result != null) {
            return result;
        }

        // Delegate to subclass for complex type deserialization
        return deserializeObject(data, type, schemaRegistryType);
    }

    /**
     * Helper method for handling primitive types and String deserialization.
     * 
     * @param <T> The type to deserialize to
     * @param data The byte array to deserialize
     * @param type The class type to deserialize to
     * @return The deserialized primitive or String, or null if not a primitive or String
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializePrimitive(byte[] data, Class<T> type) {
        // Handle String type
        if (type == String.class) {
            return (T) new String(data, StandardCharsets.UTF_8);
        }

        // Handle primitive types and their wrappers
        String str = new String(data, StandardCharsets.UTF_8);

        if (type == Integer.class || type == int.class) {
            return (T) Integer.valueOf(str);
        } else if (type == Long.class || type == long.class) {
            return (T) Long.valueOf(str);
        } else if (type == Double.class || type == double.class) {
            return (T) Double.valueOf(str);
        } else if (type == Float.class || type == float.class) {
            return (T) Float.valueOf(str);
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) Boolean.valueOf(str);
        } else if (type == Byte.class || type == byte.class) {
            return (T) Byte.valueOf(str);
        } else if (type == Short.class || type == short.class) {
            return (T) Short.valueOf(str);
        } else if (type == Character.class || type == char.class) {
            if (!str.isEmpty()) {
                return (T) Character.valueOf(str.charAt(0));
            }
            throw new IllegalArgumentException("Cannot convert empty string to char");
        }

        return null;
    }
}
