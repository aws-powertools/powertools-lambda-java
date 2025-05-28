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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.kafka.testutils.TestProductPojo;
import software.amazon.lambda.powertools.kafka.testutils.TestUtils;

class AbstractKafkaDeserializerTest {

    private TestDeserializer deserializer;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        deserializer = new TestDeserializer();
    }

    static Stream<InputType> inputTypes() {
        return Stream.of(InputType.INPUT_STREAM, InputType.STRING);
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldThrowExceptionWhenTypeIsNotConsumerRecords(InputType inputType) {
        // Given
        String json = "{}";

        // When/Then
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
            assertThatThrownBy(() -> deserializer.fromJson(inputStream, String.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Type must be ConsumerRecords<K, V>");
        } else {
            assertThatThrownBy(() -> deserializer.fromJson(json, String.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Type must be ConsumerRecords<K, V>");
        }
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldThrowExceptionWhenJsonIsInvalid(InputType inputType) {
        // Given
        String invalidJson = "{invalid json";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When/Then
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes());
            assertThatThrownBy(() -> deserializer.fromJson(inputStream, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Lambda handler input to ConsumerRecords");
        } else {
            assertThatThrownBy(() -> deserializer.fromJson(invalidJson, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Lambda handler input to ConsumerRecords");
        }
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldThrowExceptionWhenKeyDeserializationFails(InputType inputType) {
        // Given
        // Create a Kafka event with invalid Base64 for the key
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {\n" +
                "    \"test-topic-1\": [\n" +
                "      {\n" +
                "        \"topic\": \"test-topic-1\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"invalid-base64!\",\n" +
                "        \"value\": \"eyJrZXkiOiJ2YWx1ZSJ9\",\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When/Then
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            assertThatThrownBy(() -> deserializer.fromJson(inputStream, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Kafka record key");
        } else {
            assertThatThrownBy(() -> deserializer.fromJson(kafkaJson, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Kafka record key");
        }
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldThrowExceptionWhenValueDeserializationFails(InputType inputType) {
        // Given
        // Create a Kafka event with invalid Base64 for the value
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {\n" +
                "    \"test-topic-1\": [\n" +
                "      {\n" +
                "        \"topic\": \"test-topic-1\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": null,\n" +
                "        \"value\": \"invalid-base64!\",\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When/Then
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            assertThatThrownBy(() -> deserializer.fromJson(inputStream, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Kafka record value");
        } else {
            assertThatThrownBy(() -> deserializer.fromJson(kafkaJson, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Kafka record value");
        }
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldHandleNullKeyAndValue(InputType inputType) {
        // Given
        // Create a Kafka event with null key and value
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {\n" +
                "    \"test-topic-1\": [\n" +
                "      {\n" +
                "        \"topic\": \"test-topic-1\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": null,\n" +
                "        \"value\": null,\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When
        ConsumerRecords<String, TestProductPojo> records;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            records = deserializer.fromJson(inputStream, type);
        } else {
            records = deserializer.fromJson(kafkaJson, type);
        }

        // Then
        assertThat(records).isNotNull();
        TopicPartition tp = new TopicPartition("test-topic-1", 0);
        List<ConsumerRecord<String, TestProductPojo>> topicRecords = records.records(tp);
        assertThat(topicRecords).hasSize(1);

        ConsumerRecord<String, TestProductPojo> consumerRecord = topicRecords.get(0);
        assertThat(consumerRecord.key()).isNull();
        assertThat(consumerRecord.value()).isNull();
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldHandleHeadersCorrectly(InputType inputType) {
        // Given
        // Create a Kafka event with headers
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {\n" +
                "    \"test-topic-1\": [\n" +
                "      {\n" +
                "        \"topic\": \"test-topic-1\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": null,\n" +
                "        \"value\": null,\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey1\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101, 49],\n" +
                "            \"headerKey2\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101, 50]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When
        ConsumerRecords<String, TestProductPojo> records;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            records = deserializer.fromJson(inputStream, type);
        } else {
            records = deserializer.fromJson(kafkaJson, type);
        }

        // Then
        assertThat(records).isNotNull();
        TopicPartition tp = new TopicPartition("test-topic-1", 0);
        List<ConsumerRecord<String, TestProductPojo>> topicRecords = records.records(tp);
        assertThat(topicRecords).hasSize(1);

        ConsumerRecord<String, TestProductPojo> consumerRecord = topicRecords.get(0);
        assertThat(consumerRecord.headers()).isNotNull();
        assertThat(consumerRecord.headers().toArray()).hasSize(2);
        assertThat(new String(consumerRecord.headers().lastHeader("headerKey1").value())).isEqualTo("headerValue1");
        assertThat(new String(consumerRecord.headers().lastHeader("headerKey2").value())).isEqualTo("headerValue2");
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldHandleEmptyRecords(InputType inputType) {
        // Given
        // Create a Kafka event with no records
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {}\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When
        ConsumerRecords<String, TestProductPojo> records;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            records = deserializer.fromJson(inputStream, type);
        } else {
            records = deserializer.fromJson(kafkaJson, type);
        }

        // Then
        assertThat(records).isNotNull();
        assertThat(records.count()).isZero();
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldHandleNullRecords(InputType inputType) {
        // Given
        // Create a Kafka event with null records
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\"\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(String.class, TestProductPojo.class);

        // When
        ConsumerRecords<String, TestProductPojo> records;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            records = deserializer.fromJson(inputStream, type);
        } else {
            records = deserializer.fromJson(kafkaJson, type);
        }

        // Then
        assertThat(records).isNotNull();
        assertThat(records.count()).isZero();
    }

    static Stream<Arguments> primitiveTypesProvider() {
        return Stream.of(
                // For each primitive type, test with both INPUT_STREAM and STRING
                Arguments.of("String-InputStream", String.class, "test-string", "test-string", InputType.INPUT_STREAM),
                Arguments.of("String-String", String.class, "test-string", "test-string", InputType.STRING),
                Arguments.of("Integer-InputStream", Integer.class, "123", 123, InputType.INPUT_STREAM),
                Arguments.of("Integer-String", Integer.class, "123", 123, InputType.STRING),
                Arguments.of("Long-InputStream", Long.class, "123456789", 123456789L, InputType.INPUT_STREAM),
                Arguments.of("Long-String", Long.class, "123456789", 123456789L, InputType.STRING),
                Arguments.of("Double-InputStream", Double.class, "123.456", 123.456, InputType.INPUT_STREAM),
                Arguments.of("Double-String", Double.class, "123.456", 123.456, InputType.STRING),
                Arguments.of("Float-InputStream", Float.class, "123.45", 123.45f, InputType.INPUT_STREAM),
                Arguments.of("Float-String", Float.class, "123.45", 123.45f, InputType.STRING),
                Arguments.of("Boolean-InputStream", Boolean.class, "true", true, InputType.INPUT_STREAM),
                Arguments.of("Boolean-String", Boolean.class, "true", true, InputType.STRING),
                Arguments.of("Byte-InputStream", Byte.class, "127", (byte) 127, InputType.INPUT_STREAM),
                Arguments.of("Byte-String", Byte.class, "127", (byte) 127, InputType.STRING),
                Arguments.of("Short-InputStream", Short.class, "32767", (short) 32767, InputType.INPUT_STREAM),
                Arguments.of("Short-String", Short.class, "32767", (short) 32767, InputType.STRING),
                Arguments.of("Character-InputStream", Character.class, "A", 'A', InputType.INPUT_STREAM),
                Arguments.of("Character-String", Character.class, "A", 'A', InputType.STRING));
    }

    @ParameterizedTest(name = "Should handle {0}")
    @MethodSource("primitiveTypesProvider")
    <T> void shouldHandlePrimitiveTypes(String testName, Class<T> keyType, String keyValue, T expectedKey,
            InputType inputType) throws IOException {
        // Given
        // Create a TestProductPojo and serialize it to JSON
        TestProductPojo product = new TestProductPojo(123, "Test Product", 99.99, null);
        String productJson = objectMapper.writeValueAsString(product);
        String base64Value = Base64.getEncoder().encodeToString(productJson.getBytes());
        String base64Key = Base64.getEncoder().encodeToString(keyValue.getBytes());

        // Create a Kafka event with primitive type for key
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {\n" +
                "    \"test-topic-1\": [\n" +
                "      {\n" +
                "        \"topic\": \"test-topic-1\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"" + base64Key + "\",\n" +
                "        \"value\": \"" + base64Value + "\",\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(keyType, TestProductPojo.class);

        // When
        ConsumerRecords<T, TestProductPojo> records;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            records = deserializer.fromJson(inputStream, type);
        } else {
            records = deserializer.fromJson(kafkaJson, type);
        }

        // Then
        assertThat(records).isNotNull();
        TopicPartition tp = new TopicPartition("test-topic-1", 0);
        List<ConsumerRecord<T, TestProductPojo>> topicRecords = records.records(tp);
        assertThat(topicRecords).hasSize(1);

        ConsumerRecord<T, TestProductPojo> consumerRecord = topicRecords.get(0);
        assertThat(consumerRecord.key()).isEqualTo(expectedKey);
        assertThat(consumerRecord.value()).isNotNull();
        assertThat(consumerRecord.value().getId()).isEqualTo(123);
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    void shouldThrowExceptionWhenConvertingEmptyStringToChar(InputType inputType) {
        // Given
        String base64EmptyString = Base64.getEncoder().encodeToString("".getBytes());
        String kafkaJson = "{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"records\": {\n" +
                "    \"test-topic-1\": [\n" +
                "      {\n" +
                "        \"topic\": \"test-topic-1\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"" + base64EmptyString + "\",\n" +
                "        \"value\": null,\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        Type type = TestUtils.createConsumerRecordsType(Character.class, TestProductPojo.class);

        // When/Then
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(kafkaJson.getBytes());
            assertThatThrownBy(() -> deserializer.fromJson(inputStream, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Kafka record key")
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Cannot convert empty string to char");
        } else {
            assertThatThrownBy(() -> deserializer.fromJson(kafkaJson, type))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to deserialize Kafka record key")
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Cannot convert empty string to char");
        }
    }

    // Test implementation of AbstractKafkaDeserializer
    private static class TestDeserializer extends AbstractKafkaDeserializer {
        @Override
        protected <T> T deserializeComplex(byte[] data, Class<T> type) throws IOException {
            return objectMapper.readValue(data, type);
        }
    }

    enum InputType {
        INPUT_STREAM, STRING
    }
}
