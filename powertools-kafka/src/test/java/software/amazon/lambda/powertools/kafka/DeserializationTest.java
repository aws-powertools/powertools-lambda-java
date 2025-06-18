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

import java.lang.reflect.Method;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

class DeserializationTest {

    @Test
    void shouldHaveCorrectAnnotationRetention() {
        // Given
        Class<Deserialization> annotationClass = Deserialization.class;

        // When/Then
        assertThat(annotationClass.isAnnotation()).isTrue();
        assertThat(annotationClass.getAnnotation(java.lang.annotation.Retention.class).value())
                .isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
        assertThat(annotationClass.getAnnotation(java.lang.annotation.Target.class).value())
                .contains(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void shouldHaveTypeMethod() throws NoSuchMethodException {
        // Given
        Class<Deserialization> annotationClass = Deserialization.class;

        // When
        java.lang.reflect.Method typeMethod = annotationClass.getMethod("type");

        // Then
        assertThat(typeMethod.getReturnType()).isEqualTo(DeserializationType.class);
    }

    @Test
    void shouldBeAccessibleReflectivelyAtRuntime() throws NoSuchMethodException, SecurityException {
        // Given
        class TestHandler implements RequestHandler<ConsumerRecords<String, Object>, String> {
            @Override
            @Deserialization(type = DeserializationType.KAFKA_JSON)
            public String handleRequest(ConsumerRecords<String, Object> input, Context context) {
                return "OK";
            }
        }

        // When
        Method handleRequestMethod = TestHandler.class.getMethod("handleRequest", ConsumerRecords.class, Context.class);

        // Then
        Deserialization annotation = handleRequestMethod.getAnnotation(Deserialization.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.type()).isEqualTo(DeserializationType.KAFKA_JSON);
    }
}
