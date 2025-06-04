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

package software.amazon.lambda.powertools.metrics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DimensionSetTest {

    @Test
    void shouldCreateEmptyDimensionSet() {
        // When
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // Then
        assertThat(dimensionSet.getDimensions()).isEmpty();
        assertThat(dimensionSet.getDimensionKeys()).isEmpty();
    }

    @Test
    void shouldCreateDimensionSetWithSingleKeyValue() {
        // When
        DimensionSet dimensionSet = DimensionSet.of("Key", "Value");

        // Then
        assertThat(dimensionSet.getDimensions()).containsExactly(Map.entry("Key", "Value"));
        assertThat(dimensionSet.getDimensionKeys()).containsExactly("Key");
    }

    @Test
    void shouldCreateDimensionSetWithTwoKeyValues() {
        // When
        DimensionSet dimensionSet = DimensionSet.of("Key1", "Value1", "Key2", "Value2");

        // Then
        assertThat(dimensionSet.getDimensions())
                .containsEntry("Key1", "Value1")
                .containsEntry("Key2", "Value2");
        assertThat(dimensionSet.getDimensionKeys()).containsExactly("Key1", "Key2");
    }

    @Test
    void shouldCreateDimensionSetWithThreeKeyValues() {
        // When
        DimensionSet dimensionSet = DimensionSet.of(
                "Key1", "Value1",
                "Key2", "Value2",
                "Key3", "Value3");

        // Then
        assertThat(dimensionSet.getDimensions())
                .containsEntry("Key1", "Value1")
                .containsEntry("Key2", "Value2")
                .containsEntry("Key3", "Value3");
        assertThat(dimensionSet.getDimensionKeys()).containsExactly("Key1", "Key2", "Key3");
    }

    @Test
    void shouldCreateDimensionSetWithFourKeyValues() {
        // When
        DimensionSet dimensionSet = DimensionSet.of(
                "Key1", "Value1",
                "Key2", "Value2",
                "Key3", "Value3",
                "Key4", "Value4");

        // Then
        assertThat(dimensionSet.getDimensions())
                .containsEntry("Key1", "Value1")
                .containsEntry("Key2", "Value2")
                .containsEntry("Key3", "Value3")
                .containsEntry("Key4", "Value4");
        assertThat(dimensionSet.getDimensionKeys()).containsExactly("Key1", "Key2", "Key3", "Key4");
    }

    @Test
    void shouldCreateDimensionSetWithFiveKeyValues() {
        // When
        DimensionSet dimensionSet = DimensionSet.of(
                "Key1", "Value1",
                "Key2", "Value2",
                "Key3", "Value3",
                "Key4", "Value4",
                "Key5", "Value5");

        // Then
        assertThat(dimensionSet.getDimensions())
                .containsEntry("Key1", "Value1")
                .containsEntry("Key2", "Value2")
                .containsEntry("Key3", "Value3")
                .containsEntry("Key4", "Value4")
                .containsEntry("Key5", "Value5");
        assertThat(dimensionSet.getDimensionKeys()).containsExactly("Key1", "Key2", "Key3", "Key4", "Key5");
    }

    @Test
    void shouldCreateDimensionSetFromMap() {
        // Given
        Map<String, String> dimensions = Map.of(
                "Key1", "Value1",
                "Key2", "Value2");

        // When
        DimensionSet dimensionSet = DimensionSet.of(dimensions);

        // Then
        assertThat(dimensionSet.getDimensions()).isEqualTo(dimensions);
        assertThat(dimensionSet.getDimensionKeys()).containsExactlyInAnyOrder("Key1", "Key2");
    }

    @Test
    void shouldGetDimensionValue() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of("Key1", "Value1", "Key2", "Value2");

        // When
        String value = dimensionSet.getDimensionValue("Key1");

        // Then
        assertThat(value).isEqualTo("Value1");
    }

    @Test
    void shouldReturnNullForNonExistentDimension() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of("Key1", "Value1");

        // When
        String value = dimensionSet.getDimensionValue("NonExistentKey");

        // Then
        assertThat(value).isNull();
    }

    @Test
    void shouldThrowExceptionWhenExceedingMaxDimensions() {
        // Given
        // Create a dimension set with 30 dimensions (30 is maximum)
        DimensionSet dimensionSet = new DimensionSet();
        for (int i = 1; i <= 30; i++) {
            dimensionSet.addDimension("Key" + i, "Value" + i);
        }

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key31", "Value31"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot exceed 30 dimensions per dimension set");
    }

    @Test
    void shouldThrowExceptionWhenKeyIsNull() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension(null, "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenKeyIsEmpty() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("", "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenValueIsEmpty() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenKeyContainsWhitespace() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key With Space", "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot contain whitespaces: Key With Space");
    }

    @Test
    void shouldThrowExceptionWhenValueContainsWhitespace() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key", "Value With Space"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value cannot contain whitespaces: Value With Space");
    }

    @Test
    void shouldThrowExceptionWhenKeyStartsWithColon() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension(":Key", "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension key cannot start with colon: :Key");
    }

    @Test
    void shouldThrowExceptionWhenKeyExceedsMaxLength() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());
        String longKey = "a".repeat(251); // MAX_DIMENSION_NAME_LENGTH + 1

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension(longKey, "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension name exceeds maximum length of 250: " + longKey);
    }

    @Test
    void shouldThrowExceptionWhenValueExceedsMaxLength() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());
        String longValue = "a".repeat(1025); // MAX_DIMENSION_VALUE_LENGTH + 1

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key", longValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value exceeds maximum length of 1024: " + longValue);
    }

    @Test
    void shouldThrowExceptionWhenKeyContainsNonAsciiCharacters() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());
        String keyWithNonAscii = "Key\u0080"; // Non-ASCII character

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension(keyWithNonAscii, "Value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension name has invalid characters: " + keyWithNonAscii);
    }

    @Test
    void shouldThrowExceptionWhenValueContainsNonAsciiCharacters() {
        // Given
        DimensionSet dimensionSet = DimensionSet.of(Collections.emptyMap());
        String valueWithNonAscii = "Value\u0080"; // Non-ASCII character

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key", valueWithNonAscii))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dimension value has invalid characters: " + valueWithNonAscii);
    }
}
