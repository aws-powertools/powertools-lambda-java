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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class representing Lambda execution environment metadata.
 * <p>
 * This class is immutable and contains metadata retrieved from the Lambda Metadata Endpoint (LMDS).
 * Use {@link LambdaMetadataClient#get()} to obtain an instance.
 * </p>
 * <p>
 * This class is annotated with {@link JsonIgnoreProperties} to ensure forward compatibility.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * LambdaMetadata metadata = LambdaMetadataClient.get();
 * String azId = metadata.getAvailabilityZoneId();
 * }</pre>
 *
 * @see LambdaMetadataClient
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LambdaMetadata {

    @JsonProperty("AvailabilityZoneID")
    private final String availabilityZoneId;

    /**
     * Default constructor for Jackson deserialization.
     */
    public LambdaMetadata() {
        this.availabilityZoneId = null;
    }

    /**
     * Constructor with availability zone ID.
     *
     * @param availabilityZoneId the availability zone ID
     */
    public LambdaMetadata(String availabilityZoneId) {
        this.availabilityZoneId = availabilityZoneId;
    }

    /**
     * Returns the Availability Zone ID.
     * <p>
     * The Availability Zone ID is a unique identifier for the availability zone
     * where the Lambda function is executing (e.g., "use1-az1").
     * </p>
     *
     * @return the availability zone ID
     */
    public String getAvailabilityZoneId() {
        return availabilityZoneId;
    }
}
