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
        // Create a map with 9 dimensions (9 is maximum)
        Map<String, String> dimensions = Map.of(
                "Key1", "Value1", "Key2", "Value2", "Key3", "Value3", "Key4", "Value4", "Key5", "Value5",
                "Key6", "Value6", "Key7", "Value7", "Key8", "Value8", "Key9", "Value9");
        DimensionSet dimensionSet = DimensionSet.of(dimensions);

        // When/Then
        assertThatThrownBy(() -> dimensionSet.addDimension("Key10", "Value10"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot exceed 9 dimensions per dimension set");
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
                .hasMessage("Dimension value cannot be null");
    }
}
