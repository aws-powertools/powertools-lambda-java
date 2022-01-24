/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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

import java.time.Instant;
import java.util.Objects;

/**
 * Data Class for idempotency records. This is actually the item that will be stored in the persistence layer.
 */
public class DataRecord {
    private final String idempotencyKey;
    private final String status;
    private final long expiryTimestamp;
    private final String responseData;
    private final String payloadHash;

    public DataRecord(String idempotencyKey, Status status, long expiryTimestamp, String responseData, String payloadHash) {
        this.idempotencyKey = idempotencyKey;
        this.status = status.toString();
        this.expiryTimestamp = expiryTimestamp;
        this.responseData = responseData;
        this.payloadHash = payloadHash;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Check if data record is expired (based on expiration configured in the {@link software.amazon.lambda.powertools.idempotency.IdempotencyConfig})
     *
     * @return Whether the record is currently expired or not
     */
    public boolean isExpired(Instant now) {
        return expiryTimestamp != 0 && now.isAfter(Instant.ofEpochMilli(expiryTimestamp));
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

    public String getResponseData() {
        return responseData;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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

    /**
     * Status of the record:
     * <ul>
     *  <li>INPROGRESS: record initialized when function starts</li>
     *  <li>COMPLETED: record updated with the result of the function when it ends</li>
     *  <li>EXPIRED: record expired, idempotency will not happen</li>
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
