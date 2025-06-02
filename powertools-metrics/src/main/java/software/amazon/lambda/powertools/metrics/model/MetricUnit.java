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

/**
 * Metric units supported by CloudWatch
 */
public enum MetricUnit {
    SECONDS("Seconds"),
    MICROSECONDS("Microseconds"),
    MILLISECONDS("Milliseconds"),
    BYTES("Bytes"),
    KILOBYTES("Kilobytes"),
    MEGABYTES("Megabytes"),
    GIGABYTES("Gigabytes"),
    TERABYTES("Terabytes"),
    BITS("Bits"),
    KILOBITS("Kilobits"),
    MEGABITS("Megabits"),
    GIGABITS("Gigabits"),
    TERABITS("Terabits"),
    PERCENT("Percent"),
    COUNT("Count"),
    BYTES_SECOND("Bytes/Second"),
    KILOBYTES_SECOND("Kilobytes/Second"),
    MEGABYTES_SECOND("Megabytes/Second"),
    GIGABYTES_SECOND("Gigabytes/Second"),
    TERABYTES_SECOND("Terabytes/Second"),
    BITS_SECOND("Bits/Second"),
    KILOBITS_SECOND("Kilobits/Second"),
    MEGABITS_SECOND("Megabits/Second"),
    GIGABITS_SECOND("Gigabits/Second"),
    TERABITS_SECOND("Terabits/Second"),
    COUNT_SECOND("Count/Second"),
    NONE("None");

    private final String name;

    MetricUnit(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}