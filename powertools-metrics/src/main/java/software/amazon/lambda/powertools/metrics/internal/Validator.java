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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for validating metrics-related parameters.
 */
public class Validator {
    private static final int MAX_DIMENSION_NAME_LENGTH = 250;
    private static final int MAX_DIMENSION_VALUE_LENGTH = 1024;
    private static final int MAX_NAMESPACE_LENGTH = 255;
    private static final String NAMESPACE_REGEX = "^[a-zA-Z0-9._#/]+$";
    public static final long MAX_TIMESTAMP_PAST_AGE_SECONDS = TimeUnit.DAYS.toSeconds(14);
    public static final long MAX_TIMESTAMP_FUTURE_AGE_SECONDS = TimeUnit.HOURS.toSeconds(2);

    private Validator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validates that a namespace is properly specified.
     *
     * @param namespace The namespace to validate
     * @throws IllegalArgumentException if the namespace is invalid
     */
    public static void validateNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace must be specified before flushing metrics");
        }

        if (namespace.length() > MAX_NAMESPACE_LENGTH) {
            throw new IllegalArgumentException(
                    "Namespace exceeds maximum length of " + MAX_NAMESPACE_LENGTH + ": " + namespace);
        }

        if (!namespace.matches(NAMESPACE_REGEX)) {
            throw new IllegalArgumentException("Namespace contains invalid characters: " + namespace);
        }
    }

    /**
     * Validates Timestamp.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#about_timestamp">CloudWatch
     *     Timestamp</a>
     * @param timestamp Timestamp
     * @throws IllegalArgumentException if timestamp is invalid
     */
    public static void validateTimestamp(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }

        if (timestamp.isAfter(
                Instant.now().plusSeconds(MAX_TIMESTAMP_FUTURE_AGE_SECONDS))) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be more than "
                            + MAX_TIMESTAMP_FUTURE_AGE_SECONDS
                            + " seconds in the future");
        }

        if (timestamp.isBefore(
                Instant.now().minusSeconds(MAX_TIMESTAMP_PAST_AGE_SECONDS))) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be more than "
                            + MAX_TIMESTAMP_PAST_AGE_SECONDS
                            + " seconds in the past");
        }
    }

    /**
     * Validates a dimension key-value pair.
     *
     * @param key The dimension key to validate
     * @param value The dimension value to validate
     * @throws IllegalArgumentException if the key or value is invalid
     */
    public static void validateDimension(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Dimension key cannot be null or empty");
        }

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Dimension value cannot be null or empty");
        }

        if (StringUtils.containsWhitespace(key)) {
            throw new IllegalArgumentException("Dimension key cannot contain whitespaces: " + key);
        }

        if (StringUtils.containsWhitespace(value)) {
            throw new IllegalArgumentException("Dimension value cannot contain whitespaces: " + value);
        }

        if (key.startsWith(":")) {
            throw new IllegalArgumentException("Dimension key cannot start with colon: " + key);
        }

        if (key.length() > MAX_DIMENSION_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Dimension name exceeds maximum length of " + MAX_DIMENSION_NAME_LENGTH + ": " + key);
        }

        if (value.length() > MAX_DIMENSION_VALUE_LENGTH) {
            throw new IllegalArgumentException(
                    "Dimension value exceeds maximum length of " + MAX_DIMENSION_VALUE_LENGTH + ": " + value);
        }

        if (!StringUtils.isAsciiPrintable(key)) {
            throw new IllegalArgumentException("Dimension name has invalid characters: " + key);
        }

        if (!StringUtils.isAsciiPrintable(value)) {
            throw new IllegalArgumentException("Dimension value has invalid characters: " + value);
        }
    }
}
