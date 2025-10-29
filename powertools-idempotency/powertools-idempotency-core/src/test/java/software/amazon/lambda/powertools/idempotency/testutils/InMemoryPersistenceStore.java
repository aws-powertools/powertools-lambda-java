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

package software.amazon.lambda.powertools.idempotency.testutils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;

/**
 * In-memory implementation of BasePersistenceStore for testing purposes.
 */
public class InMemoryPersistenceStore extends BasePersistenceStore {
    private final Map<String, DataRecord> data = new HashMap<>();

    @Override
    public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {
        DataRecord dr = data.get(idempotencyKey);
        if (dr == null) {
            throw new IdempotencyItemNotFoundException(idempotencyKey);
        }
        return dr;
    }

    @Override
    public void putRecord(DataRecord dr, Instant now) throws IdempotencyItemAlreadyExistsException {
        if (data.containsKey(dr.getIdempotencyKey())) {
            throw new IdempotencyItemAlreadyExistsException();
        }
        data.put(dr.getIdempotencyKey(), dr);
    }

    @Override
    public void updateRecord(DataRecord dr) {
        data.put(dr.getIdempotencyKey(), dr);
    }

    @Override
    public void deleteRecord(String idempotencyKey) {
        data.remove(idempotencyKey);
    }
}
