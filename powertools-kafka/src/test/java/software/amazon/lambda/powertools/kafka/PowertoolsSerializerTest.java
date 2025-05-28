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
package software.amazon.lambda.powertools.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.kafka.testutils.TestUtils.createConsumerRecordsType;
import static software.amazon.lambda.powertools.kafka.testutils.TestUtils.serializeAvro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.kafka.serializers.PowertoolsDeserializer;
import software.amazon.lambda.powertools.kafka.testutils.TestProductPojo;

// This is testing the whole serializer end-to-end. More detailed serializer tests are placed in serializers folder.
@ExtendWith(MockitoExtension.class)
class PowertoolsSerializerTest {

    @Mock
    private PowertoolsDeserializer mockDeserializer;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Helper for parameterized tests
    static Stream<InputType> inputTypes() {
        return Stream.of(InputType.INPUT_STREAM, InputType.STRING);
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    @SetEnvironmentVariable(key = "_HANDLER", value = "")
    void shouldUseDefaultDeserializerWhenHandlerNotFound(InputType inputType) throws JsonProcessingException {
        // When
        PowertoolsSerializer serializer = new PowertoolsSerializer();

        // Then
        TestProductPojo product = new TestProductPojo(123, "Test Product", 99.99, Arrays.asList("tag1", "tag2"));
        String json = objectMapper.writeValueAsString(product);

        // This will use the Lambda default deserializer (no Kafka logic)
        TestProductPojo result;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream input = new ByteArrayInputStream(json.getBytes());
            result = serializer.fromJson(input, TestProductPojo.class);
        } else {
            result = serializer.fromJson(json, TestProductPojo.class);
        }

        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(99.99);
        assertThat(result.getTags()).containsExactly("tag1", "tag2");
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.DefaultHandler::handleRequest")
    void shouldUseLambdaDefaultDeserializer(InputType inputType) throws JsonProcessingException {
        // When
        PowertoolsSerializer serializer = new PowertoolsSerializer();

        // Then
        TestProductPojo product = new TestProductPojo(123, "Test Product", 99.99, Arrays.asList("tag1", "tag2"));
        String json = objectMapper.writeValueAsString(product);

        // This will use the Lambda default deserializer (no Kafka logic)
        TestProductPojo result;
        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream input = new ByteArrayInputStream(json.getBytes());
            result = serializer.fromJson(input, TestProductPojo.class);
        } else {
            result = serializer.fromJson(json, TestProductPojo.class);
        }

        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(99.99);
        assertThat(result.getTags()).containsExactly("tag1", "tag2");
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.JsonHandler::handleRequest")
    void shouldUseKafkaJsonDeserializer(InputType inputType) throws JsonProcessingException {
        // When
        PowertoolsSerializer serializer = new PowertoolsSerializer();

        // Create a TestProductPojo and serialize it
        TestProductPojo product = new TestProductPojo(123, "Test Product", 99.99, Arrays.asList("tag1", "tag2"));
        String productJson = objectMapper.writeValueAsString(product);
        String base64Value = Base64.getEncoder().encodeToString(productJson.getBytes());

        // Then
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
                "        \"value\": \"" + base64Value + "\",\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Type type = createConsumerRecordsType(String.class, TestProductPojo.class);

        // This should use the KafkaJsonDeserializer
        ConsumerRecords<String, TestProductPojo> records;

        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream input = new ByteArrayInputStream(kafkaJson.getBytes());
            records = serializer.fromJson(input, type);
        } else {
            records = serializer.fromJson(kafkaJson, type);
        }

        // Verify we got a valid ConsumerRecords object
        assertThat(records).isNotNull();

        // Get the record and verify its content
        TopicPartition tp = new TopicPartition("test-topic-1", 0);
        List<ConsumerRecord<String, TestProductPojo>> topicRecords = records.records(tp);
        assertThat(topicRecords).hasSize(1);

        ConsumerRecord<String, TestProductPojo> consumerRecord = topicRecords.get(0);
        TestProductPojo deserializedProduct = consumerRecord.value();

        assertThat(deserializedProduct.getId()).isEqualTo(123);
        assertThat(deserializedProduct.getName()).isEqualTo("Test Product");
        assertThat(deserializedProduct.getPrice()).isEqualTo(99.99);
        assertThat(deserializedProduct.getTags()).containsExactly("tag1", "tag2");
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.AvroHandler::handleRequest")
    void shouldUseKafkaAvroDeserializer(InputType inputType) throws IOException {
        // When
        PowertoolsSerializer serializer = new PowertoolsSerializer();

        // Create an Avro TestProduct and serialize it
        software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct product = new software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct(
                123, "Test Product", 99.99);
        String base64Value = Base64.getEncoder().encodeToString(serializeAvro(product));

        // Then
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
                "        \"value\": \"" + base64Value + "\",\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Type type = createConsumerRecordsType(String.class,
                software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct.class);

        // This should use the KafkaAvroDeserializer
        ConsumerRecords<String, software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct> records;

        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream input = new ByteArrayInputStream(kafkaJson.getBytes());
            records = serializer.fromJson(input, type);
        } else {
            records = serializer.fromJson(kafkaJson, type);
        }

        // Verify we got a valid ConsumerRecords object
        assertThat(records).isNotNull();

        // Get the record and verify its content
        TopicPartition tp = new TopicPartition("test-topic-1", 0);
        List<ConsumerRecord<String, software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct>> topicRecords = records
                .records(tp);
        assertThat(topicRecords).hasSize(1);

        ConsumerRecord<String, software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct> consumerRecord = topicRecords
                .get(0);
        software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct deserializedProduct = consumerRecord
                .value();

        assertThat(deserializedProduct.getId()).isEqualTo(123);
        assertThat(deserializedProduct.getName()).isEqualTo("Test Product");
        assertThat(deserializedProduct.getPrice()).isEqualTo(99.99);
    }

    @ParameterizedTest
    @MethodSource("inputTypes")
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.ProtobufHandler::handleRequest")
    void shouldUseKafkaProtobufDeserializer(InputType inputType) {
        // When
        PowertoolsSerializer serializer = new PowertoolsSerializer();

        // Create a Protobuf TestProduct and serialize it
        software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct product = software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct
                .newBuilder()
                .setId(123)
                .setName("Test Product")
                .setPrice(99.99)
                .build();
        String base64Value = Base64.getEncoder().encodeToString(product.toByteArray());

        // Then
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
                "        \"value\": \"" + base64Value + "\",\n" +
                "        \"headers\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Type type = createConsumerRecordsType(String.class,
                software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct.class);

        // This should use the KafkaProtobufDeserializer
        ConsumerRecords<String, software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct> records;

        if (inputType == InputType.INPUT_STREAM) {
            ByteArrayInputStream input = new ByteArrayInputStream(kafkaJson.getBytes());
            records = serializer.fromJson(input, type);
        } else {
            records = serializer.fromJson(kafkaJson, type);
        }

        // Verify we got a valid ConsumerRecords object
        assertThat(records).isNotNull();

        // Get the record and verify its content
        TopicPartition tp = new TopicPartition("test-topic-1", 0);
        List<ConsumerRecord<String, software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct>> topicRecords = records
                .records(tp);
        assertThat(topicRecords).hasSize(1);

        ConsumerRecord<String, software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct> consumerRecord = topicRecords
                .get(0);
        software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct deserializedProduct = consumerRecord
                .value();

        assertThat(deserializedProduct.getId()).isEqualTo(123);
        assertThat(deserializedProduct.getName()).isEqualTo("Test Product");
        assertThat(deserializedProduct.getPrice()).isEqualTo(99.99);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "")
    void shouldDelegateToJsonOutput() {
        // Given
        PowertoolsSerializer serializer = new PowertoolsSerializer();

        // When
        TestProductPojo product = new TestProductPojo(123, "Test Product", 99.99, Arrays.asList("tag1", "tag2"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Then
        serializer.toJson(product, output, TestProductPojo.class);
        String json = output.toString();

        // Verify the output is valid JSON
        assertThat(json).contains("\"id\":123")
                .contains("\"name\":\"Test Product\"")
                .contains("\"price\":99.99")
                .contains("\"tags\":[\"tag1\",\"tag2\"]");
    }

    private enum InputType {
        INPUT_STREAM, STRING
    }
}
