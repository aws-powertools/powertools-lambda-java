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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.lambda.powertools.kafka.testutils.TestUtils.serializeAvro;

import java.io.IOException;

import org.apache.avro.util.ClassSecurityValidator;
import org.apache.avro.util.ClassSecurityValidator.ClassSecurityPredicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.kafka.serializers.test.avro.TestCustomer;
import software.amazon.lambda.powertools.kafka.serializers.test.avro.TestOrder;
import software.amazon.lambda.powertools.kafka.serializers.test.avro.TestOrderStatus;
import software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct;
import software.amazon.lambda.powertools.kafka.testutils.TestProductPojo;

class KafkaAvroDeserializerTest {

    private KafkaAvroDeserializer deserializer;
    private ClassSecurityPredicate originalValidator;

    @BeforeEach
    void setUp() {
        originalValidator = ClassSecurityValidator.getGlobal();
        deserializer = new KafkaAvroDeserializer();
    }

    @AfterEach
    void tearDown() {
        ClassSecurityValidator.setGlobal(originalValidator);
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotAvroSpecificRecord() {
        // Given
        byte[] data = new byte[] { 1, 2, 3 };

        // When/Then
        assertThatThrownBy(() -> deserializer.deserializeObject(data, String.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE))
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("Unsupported type for Avro deserialization");
    }

    @Test
    void shouldDeserializeValidAvroData() throws IOException {
        // Given
        TestProduct product = new TestProduct(123, "Test Product", 99.99);
        byte[] avroData = serializeAvro(product);

        // When
        TestProduct result = deserializer.deserializeObject(avroData, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(99.99);
    }

    @Test
    void shouldThrowExceptionWhenDeserializingInvalidAvroData() {
        // Given
        byte[] invalidAvroData = new byte[] { 1, 2, 3, 4, 5 };

        // When/Then
        assertThatThrownBy(() -> deserializer.deserializeObject(invalidAvroData, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE))
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("Failed to deserialize Avro data");
    }

    @Test
    void shouldDeserializeRecordWithNestedRecordAndEnum() throws IOException {
        // Given
        TestOrder order = new TestOrder("order-1", new TestCustomer("Jane"), TestOrderStatus.SHIPPED);
        byte[] avroData = serializeAvro(order);

        // When
        TestOrder result = deserializer.deserializeObject(avroData, TestOrder.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE);

        // Then
        assertThat(result).isEqualTo(order);
    }

    @Test
    void shouldNotTrustClassesOutsideDeclaredSchema() throws IOException {
        // Given
        byte[] avroData = serializeAvro(new TestProduct(123, "Test Product", 99.99));

        // When
        deserializer.deserializeObject(avroData, TestProduct.class, AbstractKafkaDeserializer.SchemaRegistryType.NONE);

        // Then
        assertThatThrownBy(() -> ClassSecurityValidator.validate(TestProductPojo.class))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldKeepValidatorConfiguredBeforeFirstUse() throws IOException {
        // Given
        ClassSecurityValidator.setGlobal(ClassSecurityValidator.builder().add(TestProductPojo.class).build());
        byte[] avroData = serializeAvro(new TestProduct(123, "Test Product", 99.99));

        // When
        TestProduct result = deserializer.deserializeObject(avroData, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE);

        // Then
        assertThat(result.getId()).isEqualTo(123);
        assertThatCode(() -> {
            ClassSecurityValidator.validate(TestProductPojo.class);
            ClassSecurityValidator.validate(TestProduct.class);
        }).doesNotThrowAnyException();
    }

}
