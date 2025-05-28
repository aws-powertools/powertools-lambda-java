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
package software.amazon.lambda.powertools.kafka.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import software.amazon.lambda.powertools.kafka.DeserializationType;

class DeserializationUtilsTest {

    // NOTE: We don't use a parameterized test here because this is not compatible with the @SetEnvironmentVariable
    // annotation.
    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "")
    void shouldReturnDefaultDeserializationTypeWhenHandlerIsEmpty() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.LAMBDA_DEFAULT);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "InvalidHandlerFormat")
    void shouldReturnDefaultDeserializationTypeWhenHandlerFormatIsInvalid() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.LAMBDA_DEFAULT);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "com.example.NonExistentClass::handleRequest")
    void shouldReturnDefaultDeserializationTypeWhenClassNotFound() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.LAMBDA_DEFAULT);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "java.lang.String::toString")
    void shouldReturnDefaultDeserializationTypeWhenClassIsNotRequestHandler() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.LAMBDA_DEFAULT);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.internal.DeserializationUtilsTest$TestHandler::nonExistentMethod")
    void shouldReturnDefaultDeserializationTypeWhenMethodNotFound() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.LAMBDA_DEFAULT);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.JsonHandler::handleRequest")
    void shouldReturnJsonDeserializationTypeFromAnnotation() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.KAFKA_JSON);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.AvroHandler::handleRequest")
    void shouldReturnAvroDeserializationTypeFromAnnotation() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.KAFKA_AVRO);
    }

    @Test
    @SetEnvironmentVariable(key = "_HANDLER", value = "software.amazon.lambda.powertools.kafka.testutils.ProtobufHandler::handleRequest")
    void shouldReturnProtobufDeserializationTypeFromAnnotation() {
        // When
        DeserializationType type = DeserializationUtils.determineDeserializationType();

        // Then
        assertThat(type).isEqualTo(DeserializationType.KAFKA_PROTOBUF);
    }
}
