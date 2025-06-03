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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a set of dimensions for CloudWatch metrics
 */
public class DimensionSet {
    private static final int MAX_DIMENSION_SET_SIZE = 9;

    private final Map<String, String> dimensions = new LinkedHashMap<>();

    /**
     * Create a dimension set with a single key-value pair
     *
     * @param key   dimension key
     * @param value dimension value
     * @return a new DimensionSet
     */
    public static DimensionSet of(String key, String value) {
        DimensionSet dimensionSet = new DimensionSet();
        dimensionSet.addDimension(key, value);
        return dimensionSet;
    }

    /**
     * Create a dimension set with two key-value pairs
     *
     * @param key1   first dimension key
     * @param value1 first dimension value
     * @param key2   second dimension key
     * @param value2 second dimension value
     * @return a new DimensionSet
     */
    public static DimensionSet of(String key1, String value1, String key2, String value2) {
        DimensionSet dimensionSet = new DimensionSet();
        dimensionSet.addDimension(key1, value1);
        dimensionSet.addDimension(key2, value2);
        return dimensionSet;
    }

    /**
     * Create a dimension set with three key-value pairs
     *
     * @param key1   first dimension key
     * @param value1 first dimension value
     * @param key2   second dimension key
     * @param value2 second dimension value
     * @param key3   third dimension key
     * @param value3 third dimension value
     * @return a new DimensionSet
     */
    public static DimensionSet of(String key1, String value1, String key2, String value2, String key3, String value3) {
        DimensionSet dimensionSet = new DimensionSet();
        dimensionSet.addDimension(key1, value1);
        dimensionSet.addDimension(key2, value2);
        dimensionSet.addDimension(key3, value3);
        return dimensionSet;
    }

    /**
     * Create a dimension set with four key-value pairs
     *
     * @param key1   first dimension key
     * @param value1 first dimension value
     * @param key2   second dimension key
     * @param value2 second dimension value
     * @param key3   third dimension key
     * @param value3 third dimension value
     * @param key4   fourth dimension key
     * @param value4 fourth dimension value
     * @return a new DimensionSet
     */
    public static DimensionSet of(String key1, String value1, String key2, String value2,
            String key3, String value3, String key4, String value4) {
        DimensionSet dimensionSet = new DimensionSet();
        dimensionSet.addDimension(key1, value1);
        dimensionSet.addDimension(key2, value2);
        dimensionSet.addDimension(key3, value3);
        dimensionSet.addDimension(key4, value4);
        return dimensionSet;
    }

    /**
     * Create a dimension set with five key-value pairs
     *
     * @param key1   first dimension key
     * @param value1 first dimension value
     * @param key2   second dimension key
     * @param value2 second dimension value
     * @param key3   third dimension key
     * @param value3 third dimension value
     * @param key4   fourth dimension key
     * @param value4 fourth dimension value
     * @param key5   fifth dimension key
     * @param value5 fifth dimension value
     * @return a new DimensionSet
     */
    public static DimensionSet of(String key1, String value1, String key2, String value2,
            String key3, String value3, String key4, String value4,
            String key5, String value5) {
        DimensionSet dimensionSet = new DimensionSet();
        dimensionSet.addDimension(key1, value1);
        dimensionSet.addDimension(key2, value2);
        dimensionSet.addDimension(key3, value3);
        dimensionSet.addDimension(key4, value4);
        dimensionSet.addDimension(key5, value5);
        return dimensionSet;
    }

    /**
     * Create a dimension set from a map of key-value pairs
     *
     * @param dimensions map of dimension key-value pairs
     * @return a new DimensionSet
     */
    public static DimensionSet of(Map<String, String> dimensions) {
        DimensionSet dimensionSet = new DimensionSet();
        dimensions.forEach(dimensionSet::addDimension);
        return dimensionSet;
    }

    /**
     * Add a dimension to this dimension set
     *
     * @param key   dimension key
     * @param value dimension value
     * @return this dimension set for chaining
     * @throws IllegalArgumentException if key or value is invalid
     * @throws IllegalStateException if adding would exceed the maximum number of dimensions
     */
    public DimensionSet addDimension(String key, String value) {
        validateDimension(key, value);

        if (dimensions.size() >= MAX_DIMENSION_SET_SIZE) {
            throw new IllegalStateException(
                    "Cannot exceed " + MAX_DIMENSION_SET_SIZE + " dimensions per dimension set");
        }

        dimensions.put(key, value);
        return this;
    }

    /**
     * Get the dimension keys in this dimension set
     *
     * @return set of dimension keys
     */
    public Set<String> getDimensionKeys() {
        return dimensions.keySet();
    }

    /**
     * Get the value for a dimension key
     *
     * @param key dimension key
     * @return dimension value or null if not found
     */
    public String getDimensionValue(String key) {
        return dimensions.get(key);
    }

    /**
     * Get the dimensions as a map. Creates a shallow copy
     *
     * @return map of dimensions
     */
    public Map<String, String> getDimensions() {
        return new LinkedHashMap<>(dimensions);
    }

    private void validateDimension(String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Dimension key cannot be null or empty");
        }

        if (value == null) {
            throw new IllegalArgumentException("Dimension value cannot be null");
        }
    }
}
