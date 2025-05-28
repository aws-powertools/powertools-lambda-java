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
import static software.amazon.lambda.powertools.kafka.testutils.TestUtils.serializeAvro;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.lambda.powertools.kafka.serializers.test.avro.TestProduct;

class KafkaAvroDeserializerTest {

    private KafkaAvroDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new KafkaAvroDeserializer();
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotAvroSpecificRecord() {
        // Given
        byte[] data = new byte[] { 1, 2, 3 };

        // When/Then
        assertThatThrownBy(() -> deserializer.deserializeComplex(data, String.class))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unsupported type for Avro deserialization");
    }

    @Test
    void shouldDeserializeValidAvroData() throws IOException {
        // Given
        TestProduct product = new TestProduct(123, "Test Product", 99.99);
        byte[] avroData = serializeAvro(product);

        // When
        TestProduct result = deserializer.deserializeComplex(avroData, TestProduct.class);

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
        assertThatThrownBy(() -> deserializer.deserializeComplex(invalidAvroData, TestProduct.class))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to deserialize Avro data");
    }

}
