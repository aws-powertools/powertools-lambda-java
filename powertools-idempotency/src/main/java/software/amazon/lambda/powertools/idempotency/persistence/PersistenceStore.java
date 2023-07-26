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

import java.time.Instant;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;

/**
 * Persistence layer that will store the idempotency result. In order to provide another
 * implementation, extends {@link BasePersistenceStore}.
 */
public interface PersistenceStore {

    /**
     * Retrieve item from persistence store using idempotency key and return it as a DataRecord
     * instance.
     *
     * @param idempotencyKey the key of the record
     * @return DataRecord representation of existing record found in persistence store
     * @throws IdempotencyItemNotFoundException Exception thrown if no record exists in persistence
     *                                          store with the idempotency key
     */
    DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException;

    /**
     * Add a DataRecord to persistence store if it does not already exist with that key
     *
     * @param record DataRecord instance
     * @param now
     * @throws IdempotencyItemAlreadyExistsException if a non-expired entry already exists.
     */
    void putRecord(DataRecord record, Instant now) throws IdempotencyItemAlreadyExistsException;

    /**
     * Update item in persistence store
     *
     * @param record DataRecord instance
     */
    void updateRecord(DataRecord record);

    /**
     * Remove item from persistence store
     *
     * @param idempotencyKey the key of the record
     */
    void deleteRecord(String idempotencyKey);
}
