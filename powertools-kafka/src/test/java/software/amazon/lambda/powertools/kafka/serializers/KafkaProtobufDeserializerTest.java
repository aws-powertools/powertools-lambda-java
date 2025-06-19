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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.CodedOutputStream;

import software.amazon.lambda.powertools.kafka.serializers.test.protobuf.TestProduct;

class KafkaProtobufDeserializerTest {

    private KafkaProtobufDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new KafkaProtobufDeserializer();
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotProtobufMessage() {
        // Given
        byte[] data = new byte[] { 1, 2, 3 };

        // When/Then
        assertThatThrownBy(() -> deserializer.deserializeObject(data, String.class))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unsupported type for Protobuf deserialization");
    }

    @Test
    void shouldDeserializeValidProtobufData() throws IOException {
        // Given
        TestProduct product = TestProduct.newBuilder()
                .setId(123)
                .setName("Test Product")
                .setPrice(99.99)
                .build();
        byte[] protobufData = product.toByteArray();

        // When
        TestProduct result = deserializer.deserializeObject(protobufData, TestProduct.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(99.99);
    }

    @Test
    void shouldThrowExceptionWhenDeserializingInvalidProtobufData() {
        // Given
        byte[] invalidProtobufData = new byte[] { 1, 2, 3, 4, 5 };

        // When/Then
        assertThatThrownBy(() -> deserializer.deserializeObject(invalidProtobufData, TestProduct.class))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to deserialize Protobuf data");
    }

    @Test
    void shouldDeserializeProtobufDataWithSimpleMessageIndex() throws IOException {
        // Given
        TestProduct product = TestProduct.newBuilder()
                .setId(456)
                .setName("Simple Index Product")
                .setPrice(199.99)
                .build();

        // Create protobuf data with simple message index (single 0)
        byte[] protobufDataWithSimpleIndex = createProtobufDataWithSimpleMessageIndex(product);

        // When
        TestProduct result = deserializer.deserializeObject(protobufDataWithSimpleIndex, TestProduct.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(456);
        assertThat(result.getName()).isEqualTo("Simple Index Product");
        assertThat(result.getPrice()).isEqualTo(199.99);
    }

    @Test
    void shouldDeserializeProtobufDataWithComplexMessageIndex() throws IOException {
        // Given
        TestProduct product = TestProduct.newBuilder()
                .setId(789)
                .setName("Complex Index Product")
                .setPrice(299.99)
                .build();

        // Create protobuf data with complex message index (array [1,0])
        byte[] protobufDataWithComplexIndex = createProtobufDataWithComplexMessageIndex(product);

        // When
        TestProduct result = deserializer.deserializeObject(protobufDataWithComplexIndex, TestProduct.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(789);
        assertThat(result.getName()).isEqualTo("Complex Index Product");
        assertThat(result.getPrice()).isEqualTo(299.99);
    }

    private byte[] createProtobufDataWithSimpleMessageIndex(TestProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(baos);

        // Write simple message index (single 0)
        codedOutput.writeUInt32NoTag(0);

        // Write the protobuf data
        product.writeTo(codedOutput);

        codedOutput.flush();
        return baos.toByteArray();
    }

    private byte[] createProtobufDataWithComplexMessageIndex(TestProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(baos);

        // Write complex message index array [1,0]
        codedOutput.writeUInt32NoTag(2); // Array length
        codedOutput.writeUInt32NoTag(1); // First index value
        codedOutput.writeUInt32NoTag(0); // Second index value

        // Write the protobuf data
        product.writeTo(codedOutput);

        codedOutput.flush();
        return baos.toByteArray();
    }
}
