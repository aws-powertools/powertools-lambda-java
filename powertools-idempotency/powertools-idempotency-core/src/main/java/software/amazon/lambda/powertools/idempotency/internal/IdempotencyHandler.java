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

package software.amazon.lambda.powertools.idempotency.internal;

import static software.amazon.lambda.powertools.idempotency.persistence.DataRecord.Status.EXPIRED;
import static software.amazon.lambda.powertools.idempotency.persistence.DataRecord.Status.INPROGRESS;

import java.time.Instant;
import java.util.OptionalInt;
import java.util.function.BiFunction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyAlreadyInProgressException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyInconsistentStateException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyKeyException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyPersistenceLayerException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyValidationException;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * Internal class that will handle the Idempotency, and use the
 * {@link software.amazon.lambda.powertools.idempotency.persistence.PersistenceStore}
 * to store the result of previous calls.
 */
public class IdempotencyHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyHandler.class);
    private static final int MAX_RETRIES = 2;

    private final ProceedingJoinPoint pjp;
    private final JsonNode data;
    private final BasePersistenceStore persistenceStore;
    private final Context lambdaContext;

    public IdempotencyHandler(ProceedingJoinPoint pjp, String functionName, JsonNode payload, Context lambdaContext) {
        this.pjp = pjp;
        this.data = payload;
        this.lambdaContext = lambdaContext;
        persistenceStore = Idempotency.getInstance().getPersistenceStore();
        persistenceStore.configure(Idempotency.getInstance().getConfig(), functionName);
    }

    /**
     * Main entry point for handling idempotent execution of a function.
     *
     * @return function response
     */
    public Object handle() throws Throwable {
        // IdempotencyInconsistentStateException can happen under rare but expected cases
        // when persistent state changes in the small time between put & get requests.
        // In most cases we can retry successfully on this exception.
        for (int i = 0; true; i++) {
            try {
                return processIdempotency();
            } catch (IdempotencyInconsistentStateException e) {
                if (i == MAX_RETRIES) {
                    throw e;
                }
            }
        }
    }

    /**
     * Process the function with idempotency
     *
     * @return function response
     */
    private Object processIdempotency() throws Throwable {
        try {
            // We call saveInProgress first as an optimization for the most common case where no idempotent record
            // already exists. If it succeeds, there's no need to call getRecord.
            persistenceStore.saveInProgress(data, Instant.now(), getRemainingTimeInMillis());
        } catch (IdempotencyItemAlreadyExistsException iaee) {
            DataRecord record = getIdempotencyRecord();
            if (record != null) {
                return handleForStatus(record);
            }
        } catch (IdempotencyKeyException ike) {
            throw ike;
        } catch (Exception e) {
            throw new IdempotencyPersistenceLayerException(
                    "Failed to save in progress record to idempotency store. If you believe this is a Powertools for AWS Lambda (Java) bug, please open an issue.",
                    e);
        }
        return getFunctionResponse();
    }

    /**
     * Tries to determine the remaining time available for the current lambda invocation.
     * Currently, it only works if the idempotent handler decorator is used or using
     * {@link Idempotency#registerLambdaContext(Context)}
     *
     * @return the remaining time in milliseconds or empty if the context was not provided/found
     */
    private OptionalInt getRemainingTimeInMillis() {
        if (lambdaContext != null) {
            return OptionalInt.of(lambdaContext.getRemainingTimeInMillis());
        } else {
            LOG.warn("Couldn't determine the remaining time left. Did you call registerLambdaContext on Idempotency?");
        }
        return OptionalInt.empty();
    }

    /**
     * Retrieve the idempotency record from the persistence layer.
     *
     * @return the record if available, potentially null
     */
    private DataRecord getIdempotencyRecord() {
        try {
            return persistenceStore.getRecord(data, Instant.now());
        } catch (IdempotencyItemNotFoundException e) {
            // This code path will only be triggered if the record is removed between saveInProgress and getRecord
            LOG.debug("An existing idempotency record was deleted before we could fetch it");
            throw new IdempotencyInconsistentStateException("saveInProgress and getRecord return inconsistent results",
                    e);
        } catch (IdempotencyValidationException | IdempotencyKeyException vke) {
            throw vke;
        } catch (Exception e) {
            throw new IdempotencyPersistenceLayerException(
                    "Failed to get record from idempotency store. If you believe this is a Powertools for AWS Lambda (Java) bug, please open an issue.",
                    e);
        }
    }

    /**
     * Take appropriate action based on data_record's status
     *
     * @param record
     *            DataRecord
     * @return Function's response previously used for this idempotency key, if it has successfully executed already.
     */
    private Object handleForStatus(DataRecord record) {
        // This code path will only be triggered if the record becomes expired between the saveInProgress call and here
        if (EXPIRED.equals(record.getStatus())) {
            throw new IdempotencyInconsistentStateException("saveInProgress and getRecord return inconsistent results");
        }

        if (INPROGRESS.equals(record.getStatus())) {
            if (record.getInProgressExpiryTimestamp().isPresent()
                    && record.getInProgressExpiryTimestamp().getAsLong() < Instant.now().toEpochMilli()) {
                throw new IdempotencyInconsistentStateException(
                        "Item should have been expired in-progress because it already time-outed.");
            }
            throw new IdempotencyAlreadyInProgressException(
                    "Execution already in progress with idempotency key: " + record.getIdempotencyKey());
        }

        Class<?> returnType = ((MethodSignature) pjp.getSignature()).getReturnType();
        try {
            LOG.debug("Response for key '{}' retrieved from idempotency store, skipping the function",
                    record.getIdempotencyKey());

            final BiFunction<Object, DataRecord, Object> responseHook = Idempotency.getInstance().getConfig()
                    .getResponseHook();
            final Object responseData;

            if (returnType.equals(String.class)) {
                // Primitive String data will be returned raw and not de-serialized from JSON.
                responseData = record.getResponseData();
            } else {
                responseData = JsonConfig.get().getObjectMapper().reader().readValue(record.getResponseData(),
                        returnType);
            }

            if (responseHook != null) {
                LOG.debug("Applying user-defined response hook to idempotency data before returning.");
                return responseHook.apply(responseData, record);
            }

            return responseData;
        } catch (Exception e) {
            throw new IdempotencyPersistenceLayerException(
                    "Unable to get function response as " + returnType.getSimpleName(), e);
        }
    }

    private Object getFunctionResponse() throws Throwable {
        Object response;
        try {
            response = pjp.proceed(pjp.getArgs());
        } catch (Throwable handlerException) {
            // We need these nested blocks to preserve function's exception in case the persistence store operation
            // also raises an exception
            try {
                persistenceStore.deleteRecord(data, handlerException);
            } catch (IdempotencyKeyException ke) {
                throw ke;
            } catch (Exception e) {
                throw new IdempotencyPersistenceLayerException(
                        "Failed to delete record from idempotency store. If you believe this is a Powertools for AWS Lambda (Java) bug, please open an issue.",
                        e);
            }
            throw handlerException;
        }

        try {
            persistenceStore.saveSuccess(data, response, Instant.now());
        } catch (Exception e) {
            throw new IdempotencyPersistenceLayerException(
                    "Failed to update record state to success in idempotency store. If you believe this is a Powertools for AWS Lambda (Java) bug, please open an issue.",
                    e);
        }
        return response;
    }

}
