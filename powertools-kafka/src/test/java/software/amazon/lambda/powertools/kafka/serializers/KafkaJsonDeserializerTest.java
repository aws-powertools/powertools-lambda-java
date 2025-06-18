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

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.kafka.testutils.TestProductPojo;

class KafkaJsonDeserializerTest {

    private KafkaJsonDeserializer deserializer;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        deserializer = new KafkaJsonDeserializer();
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNotSupportedForJson() {
        // Given
        byte[] data = new byte[] { 1, 2, 3 };

        // When/Then
        assertThatThrownBy(() -> deserializer.deserializeObject(data, Object.class))
                .isInstanceOf(JsonParseException.class);
    }

    @Test
    void shouldDeserializeValidJsonData() throws IOException {
        // Given
        TestProductPojo product = new TestProductPojo(123, "Test Product", 99.99, Arrays.asList("tag1", "tag2"));
        byte[] jsonData = objectMapper.writeValueAsBytes(product);

        // When
        TestProductPojo result = deserializer.deserializeObject(jsonData, TestProductPojo.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(123);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(99.99);
        assertThat(result.getTags()).containsExactly("tag1", "tag2");
    }

}
