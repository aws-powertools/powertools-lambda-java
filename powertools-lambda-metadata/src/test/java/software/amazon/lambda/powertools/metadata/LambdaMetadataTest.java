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
 *
 */

package software.amazon.lambda.powertools.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class LambdaMetadataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void defaultConstructor_shouldCreateInstanceWithNullValues() {
        // When
        LambdaMetadata metadata = new LambdaMetadata();

        // Then
        assertThat(metadata.getAvailabilityZoneId()).isNull();
    }

    @Test
    void constructor_withAvailabilityZoneId_shouldSetValue() {
        // When
        LambdaMetadata metadata = new LambdaMetadata("use1-az1");

        // Then
        assertThat(metadata.getAvailabilityZoneId()).isEqualTo("use1-az1");
    }

    @Test
    void deserialize_shouldMapJsonProperty() throws Exception {
        // Given
        String json = "{\"AvailabilityZoneID\": \"euw1-az3\"}";

        // When
        LambdaMetadata metadata = objectMapper.readValue(json, LambdaMetadata.class);

        // Then
        assertThat(metadata.getAvailabilityZoneId()).isEqualTo("euw1-az3");
    }

    @Test
    void deserialize_shouldIgnoreUnknownFields() throws Exception {
        // Given
        String json = "{\"AvailabilityZoneID\": \"apne1-az1\", \"UnknownField\": \"value\", \"AnotherField\": 123}";

        // When
        LambdaMetadata metadata = objectMapper.readValue(json, LambdaMetadata.class);

        // Then
        assertThat(metadata.getAvailabilityZoneId()).isEqualTo("apne1-az1");
    }
}
