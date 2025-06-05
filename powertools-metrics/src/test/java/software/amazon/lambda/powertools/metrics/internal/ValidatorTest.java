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

package software.amazon.lambda.powertools.metrics.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ValidatorTest {

    @Test
    void shouldThrowExceptionWhenNamespaceIsNull() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateNamespace(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Namespace must be specified before flushing metrics");
    }

    @Test
    void shouldThrowExceptionWhenNamespaceIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateNamespace(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Namespace must be specified before flushing metrics");
    }

    @Test
    void shouldThrowExceptionWhenNamespaceIsBlank() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateNamespace("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Namespace must be specified before flushing metrics");
    }

    @Test
    void shouldThrowExceptionWhenNamespaceExceedsMaxLength() {
        // Given
        String tooLongNamespace = "a".repeat(256);

        // When/Then
        assertThatThrownBy(() -> Validator.validateNamespace(tooLongNamespace))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Namespace exceeds maximum length of 255");
    }

    @Test
    void shouldThrowExceptionWhenNamespaceContainsInvalidCharacters() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateNamespace("Invalid Namespace"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Namespace contains invalid characters");
    }

    @Test
    void shouldAcceptValidNamespace() {
        // When/Then
        assertThatCode(() -> Validator.validateNamespace("Valid.Namespace_123#/"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionWhenDimensionKeyIsNull() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension(null, "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenDimensionKeyIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("", "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenDimensionValueIsNull() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("Key", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenDimensionValueIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("Key", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenDimensionKeyContainsWhitespace() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("Key With Space", "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot contain whitespaces: Key With Space");
    }

    @Test
    void shouldThrowExceptionWhenDimensionValueContainsWhitespace() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("Key", "Value With Space"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value cannot contain whitespaces: Value With Space");
    }

    @Test
    void shouldThrowExceptionWhenDimensionKeyStartsWithColon() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension(":Key", "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot start with colon: :Key");
    }

    @Test
    void shouldThrowExceptionWhenDimensionKeyExceedsMaxLength() {
        // Given
        String longKey = "a".repeat(251); // MAX_DIMENSION_NAME_LENGTH + 1

        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension(longKey, "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension name exceeds maximum length of 250: " + longKey);
    }

    @Test
    void shouldThrowExceptionWhenDimensionValueExceedsMaxLength() {
        // Given
        String longValue = "a".repeat(1025); // MAX_DIMENSION_VALUE_LENGTH + 1

        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("Key", longValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value exceeds maximum length of 1024: " + longValue);
    }

    @Test
    void shouldThrowExceptionWhenDimensionKeyContainsNonAsciiCharacters() {
        // Given
        String keyWithNonAscii = "Key\u0080"; // Non-ASCII character

        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension(keyWithNonAscii, "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension name has invalid characters: " + keyWithNonAscii);
    }

    @Test
    void shouldThrowExceptionWhenDimensionValueContainsNonAsciiCharacters() {
        // Given
        String valueWithNonAscii = "Value\u0080"; // Non-ASCII character

        // When/Then
        assertThatThrownBy(() -> Validator.validateDimension("Key", valueWithNonAscii))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value has invalid characters: " + valueWithNonAscii);
    }

    @Test
    void shouldAcceptValidDimension() {
        // When/Then
        assertThatCode(() -> Validator.validateDimension("ValidKey", "ValidValue"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionWhenTimestampIsNull() {
        // When/Then
        assertThatThrownBy(() -> Validator.validateTimestamp(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Timestamp cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenTimestampIsTooFarInFuture() {
        // Given
        Instant futureTooFar = Instant.now().plusSeconds(Validator.MAX_TIMESTAMP_FUTURE_AGE_SECONDS + 1);

        // When/Then
        assertThatThrownBy(() -> Validator.validateTimestamp(futureTooFar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Timestamp cannot be more than " + Validator.MAX_TIMESTAMP_FUTURE_AGE_SECONDS
                        + " seconds in the future");
    }

    @Test
    void shouldThrowExceptionWhenTimestampIsTooFarInPast() {
        // Given
        Instant pastTooFar = Instant.now().minusSeconds(Validator.MAX_TIMESTAMP_PAST_AGE_SECONDS + 1);

        // When/Then
        assertThatThrownBy(() -> Validator.validateTimestamp(pastTooFar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Timestamp cannot be more than " + Validator.MAX_TIMESTAMP_PAST_AGE_SECONDS
                        + " seconds in the past");
    }

    @Test
    void shouldAcceptValidTimestamp() {
        // Given
        Instant validTimestamp = Instant.now();

        // When/Then
        assertThatCode(() -> Validator.validateTimestamp(validTimestamp))
                .doesNotThrowAnyException();
    }
}
