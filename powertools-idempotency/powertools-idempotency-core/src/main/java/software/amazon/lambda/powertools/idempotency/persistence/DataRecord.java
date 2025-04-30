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

package software.amazon.lambda.powertools.idempotency.persistence;

import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Data Class for idempotency records. This is actually the item that will be stored in the persistence layer.
 */
public class DataRecord {
    private final String idempotencyKey;
    private final String status;

    /**
     * This field is controlling how long the result of the idempotent
     * event is cached. It is stored in _seconds since epoch_.
     * <p>
     * DynamoDB's TTL mechanism is used to remove the record once the
     * expiry has been reached, and subsequent execution of the request
     * will be permitted. The user must configure this on their table.
     */
    private final long expiryTimestamp;
    private final String responseData;
    private final String payloadHash;

    /**
     * The in-progress field is set to the remaining lambda execution time
     * when the record is created.
     * This field is stored in _milliseconds since epoch_.
     * <p>
     * This ensures that:
     * <p>
     * 1/ other concurrently executing requests are blocked from starting
     * 2/ if a lambda times out, subsequent requests will be allowed again, despite
     * the fact that the idempotency record is already in the table
     */
    private final OptionalLong inProgressExpiryTimestamp;

    public DataRecord(String idempotencyKey, Status status, long expiryTimestamp, String responseData,
            String payloadHash) {
        this.idempotencyKey = idempotencyKey;
        this.status = status.toString();
        this.expiryTimestamp = expiryTimestamp;
        this.responseData = responseData;
        this.payloadHash = payloadHash;
        this.inProgressExpiryTimestamp = OptionalLong.empty();
    }

    public DataRecord(String idempotencyKey, Status status, long expiryTimestamp, String responseData,
            String payloadHash, OptionalLong inProgressExpiryTimestamp) {
        this.idempotencyKey = idempotencyKey;
        this.status = status.toString();
        this.expiryTimestamp = expiryTimestamp;
        this.responseData = responseData;
        this.payloadHash = payloadHash;
        this.inProgressExpiryTimestamp = inProgressExpiryTimestamp;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Check if data record is expired (based on expiration configured in the {@link IdempotencyConfig})
     *
     * @return Whether the record is currently expired or not
     */
    public boolean isExpired(Instant now) {
        return expiryTimestamp != 0 && now.isAfter(Instant.ofEpochSecond(expiryTimestamp));
    }

    public Status getStatus() {
        Instant now = Instant.now();
        if (isExpired(now)) {
            return Status.EXPIRED;
        } else {
            return Status.valueOf(status);
        }
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public OptionalLong getInProgressExpiryTimestamp() {
        return inProgressExpiryTimestamp;
    }

    public String getResponseData() {
        return responseData;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataRecord record = (DataRecord) o;
        return expiryTimestamp == record.expiryTimestamp
                && idempotencyKey.equals(record.idempotencyKey)
                && status.equals(record.status)
                && Objects.equals(responseData, record.responseData)
                && Objects.equals(payloadHash, record.payloadHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idempotencyKey, status, expiryTimestamp, responseData, payloadHash);
    }

    @Override
    public String toString() {
        return "DataRecord{" +
                "idempotencyKey='" + idempotencyKey + '\'' +
                ", status='" + status + '\'' +
                ", expiryTimestamp=" + expiryTimestamp +
                ", payloadHash='" + payloadHash + '\'' +
                '}';
    }

    /**
     * Status of the record:
     * <ul>
     * <li>INPROGRESS: record initialized when function starts</li>
     * <li>COMPLETED: record updated with the result of the function when it ends</li>
     * <li>EXPIRED: record expired, idempotency will not happen</li>
     * </ul>
     */
    public enum Status {
        INPROGRESS("INPROGRESS"), COMPLETED("COMPLETED"), EXPIRED("EXPIRED");

        private final String status;

        Status(String status) {
            this.status = status;
        }

        public String toString() {
            return status;
        }
    }
}
