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

package software.amazon.lambda.powertools.metadata.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LambdaMetadataExceptionTest {

    @Test
    void constructor_withMessage_shouldSetMessage() {
        // When
        LambdaMetadataException exception = new LambdaMetadataException("Test message");

        // Then
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(-1);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_shouldSetBoth() {
        // Given
        Throwable cause = new RuntimeException("Root cause");

        // When
        LambdaMetadataException exception = new LambdaMetadataException("Test message", cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getStatusCode()).isEqualTo(-1);
    }

    @Test
    void constructor_withMessageAndStatusCode_shouldSetBoth() {
        // When
        LambdaMetadataException exception = new LambdaMetadataException("Test message", 500);

        // Then
        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.getCause()).isNull();
    }
}
