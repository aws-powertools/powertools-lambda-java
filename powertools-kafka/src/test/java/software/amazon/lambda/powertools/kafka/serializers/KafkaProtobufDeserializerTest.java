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
import java.nio.ByteBuffer;

import org.apache.kafka.common.utils.ByteUtils;
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
        assertThatThrownBy(() -> deserializer.deserializeObject(data, String.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE))
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
        TestProduct result = deserializer.deserializeObject(protobufData, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE);

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
        assertThatThrownBy(() -> deserializer.deserializeObject(invalidProtobufData, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.NONE))
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("Failed to deserialize Protobuf data");
    }

    @Test
    void shouldDeserializeProtobufDataWithSimpleMessageIndexGlue() throws IOException {
        // Given
        TestProduct product = TestProduct.newBuilder()
                .setId(456)
                .setName("Simple Index Product")
                .setPrice(199.99)
                .build();

        // Create protobuf data with simple message index (single 0)
        byte[] protobufDataWithSimpleIndex = createProtobufDataWithGlueMagicByte(product);

        // When
        TestProduct result = deserializer.deserializeObject(protobufDataWithSimpleIndex, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.GLUE);

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

        // Create protobuf data with complex message index (array [2,2])
        byte[] protobufDataWithComplexIndex = createProtobufDataWithComplexMessageIndexConfluent(product);

        // When
        TestProduct result = deserializer.deserializeObject(protobufDataWithComplexIndex, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.CONFLUENT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(789);
        assertThat(result.getName()).isEqualTo("Complex Index Product");
        assertThat(result.getPrice()).isEqualTo(299.99);
    }

    @Test
    void shouldDeserializeProtobufDataWithSimpleMessageIndexConfluent() throws IOException {
        // Given
        TestProduct product = TestProduct.newBuilder()
                .setId(789)
                .setName("Complex Index Product")
                .setPrice(299.99)
                .build();

        // Create protobuf data with simple message index for Confluent
        byte[] protobufDataWithComplexIndex = createProtobufDataWithSimpleMessageIndexConfluent(product);

        // When
        TestProduct result = deserializer.deserializeObject(protobufDataWithComplexIndex, TestProduct.class,
                AbstractKafkaDeserializer.SchemaRegistryType.CONFLUENT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(789);
        assertThat(result.getName()).isEqualTo("Complex Index Product");
        assertThat(result.getPrice()).isEqualTo(299.99);
    }

    private byte[] createProtobufDataWithGlueMagicByte(TestProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(baos);

        // Write simple message index for Glue (single UInt32)
        codedOutput.writeUInt32NoTag(1);

        // Write the protobuf data
        product.writeTo(codedOutput);

        codedOutput.flush();
        return baos.toByteArray();
    }

    private byte[] createProtobufDataWithSimpleMessageIndexConfluent(TestProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write optimized simple message index for Confluent (single 0 byte for [0])
        // https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format
        baos.write(0);

        // Write the protobuf data
        baos.write(product.toByteArray());

        return baos.toByteArray();
    }

    private byte[] createProtobufDataWithComplexMessageIndexConfluent(TestProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write complex message index array [1,0] using ByteUtils
        // https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteUtils.writeVarint(2, buffer); // Array length
        ByteUtils.writeVarint(1, buffer); // First index value
        ByteUtils.writeVarint(0, buffer); // Second index value

        buffer.flip();
        byte[] indexData = new byte[buffer.remaining()];
        buffer.get(indexData);
        baos.write(indexData);

        // Write the protobuf data
        baos.write(product.toByteArray());

        return baos.toByteArray();
    }
}
