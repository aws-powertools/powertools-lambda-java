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

import org.junit.jupiter.api.Test;

// Mainly present to remind us to write unit tests once we add support for a new Deserializer. If we add a new type in 
// the enum it will fail this test.
class DeserializationTypeTest {

    @Test
    void shouldHaveExpectedEnumValues() {
        // Given/When
        DeserializationType[] values = DeserializationType.values();

        // Then
        assertThat(values).contains(
                DeserializationType.LAMBDA_DEFAULT,
                DeserializationType.KAFKA_JSON,
                DeserializationType.KAFKA_AVRO,
                DeserializationType.KAFKA_PROTOBUF);
    }

    @Test
    void shouldBeAbleToValueOf() {
        // Given/When
        DeserializationType jsonType = DeserializationType.valueOf("KAFKA_JSON");
        DeserializationType avroType = DeserializationType.valueOf("KAFKA_AVRO");
        DeserializationType protobufType = DeserializationType.valueOf("KAFKA_PROTOBUF");
        DeserializationType defaultType = DeserializationType.valueOf("LAMBDA_DEFAULT");

        // Then
        assertThat(jsonType).isEqualTo(DeserializationType.KAFKA_JSON);
        assertThat(avroType).isEqualTo(DeserializationType.KAFKA_AVRO);
        assertThat(protobufType).isEqualTo(DeserializationType.KAFKA_PROTOBUF);
        assertThat(defaultType).isEqualTo(DeserializationType.LAMBDA_DEFAULT);
    }
}
