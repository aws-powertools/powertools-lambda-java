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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.burt.jmespath.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyKeyException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyValidationException;
import software.amazon.lambda.powertools.idempotency.internal.cache.LRUCache;
import software.amazon.lambda.powertools.utilities.JsonConfig;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static software.amazon.lambda.powertools.common.internal.LambdaConstants.LAMBDA_FUNCTION_NAME_ENV;

/**
 * Persistence layer that will store the idempotency result.
 * Base implementation. See {@link DynamoDBPersistenceStore} for an implementation (default one)
 * Extends this class to use your own implementation (DocumentDB, Elasticache, ...)
 */
public abstract class BasePersistenceStore implements PersistenceStore {

    private static final Logger LOG = LoggerFactory.getLogger(BasePersistenceStore.class);
    protected boolean payloadValidationEnabled = false;
    private String functionName = "";
    private boolean configured = false;
    private long expirationInSeconds = 60 * 60L; // 1 hour default
    private boolean useLocalCache = false;
    private LRUCache<String, DataRecord> cache;
    private String eventKeyJMESPath;
    private Expression<JsonNode> eventKeyCompiledJMESPath;
    private Expression<JsonNode> validationKeyJMESPath;
    private boolean throwOnNoIdempotencyKey = false;
    private String hashFunctionName;

    /**
     * Initialize the base persistence layer from the configuration settings
     *
     * @param config       Idempotency configuration settings
     * @param functionName The name of the function being decorated
     */
    public void configure(IdempotencyConfig config, String functionName) {
        String funcEnv = System.getenv(LAMBDA_FUNCTION_NAME_ENV);
        this.functionName = funcEnv != null ? funcEnv : "testFunction";
        if (functionName != null && !functionName.isEmpty()) {
            this.functionName += "." + functionName;
        }

        if (configured) {
            // prevent being reconfigured multiple times
            return;
        }

        eventKeyJMESPath = config.getEventKeyJMESPath();
        if (eventKeyJMESPath != null) {
            eventKeyCompiledJMESPath = JsonConfig.get().getJmesPath().compile(eventKeyJMESPath);
        }
        if (config.getPayloadValidationJMESPath() != null) {
            validationKeyJMESPath = JsonConfig.get().getJmesPath().compile(config.getPayloadValidationJMESPath());
            payloadValidationEnabled = true;
        }
        throwOnNoIdempotencyKey = config.throwOnNoIdempotencyKey();

        useLocalCache = config.useLocalCache();
        if (useLocalCache) {
            cache = new LRUCache<>(config.getLocalCacheMaxItems());
        }
        expirationInSeconds = config.getExpirationInSeconds();
        hashFunctionName = config.getHashFunction();
        configured = true;
    }

    /**
     * Save record of function's execution completing successfully
     *
     * @param data   Payload
     * @param result the response from the function
     */
    public void saveSuccess(JsonNode data, Object result, Instant now) {
        ObjectWriter writer = JsonConfig.get().getObjectMapper().writer();
        try {
            String responseJson;
            if (result instanceof String) {
                responseJson = (String) result;
            } else {
                responseJson = writer.writeValueAsString(result);
            }
            Optional<String> hashedIdempotencyKey = getHashedIdempotencyKey(data);
            if (!hashedIdempotencyKey.isPresent()) {
                // missing idempotency key => non-idempotent transaction, we do not store the data, simply return
                return;
            }
            DataRecord dataRecord = new DataRecord(
                    hashedIdempotencyKey.get(),
                    DataRecord.Status.COMPLETED,
                    getExpiryEpochSecond(now),
                    responseJson,
                    getHashedPayload(data)
            );
            LOG.debug("Function successfully executed. Saving record to persistence store with idempotency key: {}",
                    dataRecord.getIdempotencyKey());
            updateRecord(dataRecord);
            saveToCache(dataRecord);
        } catch (JsonProcessingException e) {
            // TODO : throw ?
            throw new RuntimeException("Error while serializing the response", e);
        }
    }

    /**
     * Save record of function's execution being in progress
     *
     * @param data Payload
     * @param now
     */
    public void saveInProgress(JsonNode data, Instant now, OptionalInt remainingTimeInMs)
            throws IdempotencyItemAlreadyExistsException {
        Optional<String> hashedIdempotencyKey = getHashedIdempotencyKey(data);
        if (!hashedIdempotencyKey.isPresent()) {
            // missing idempotency key => non-idempotent transaction, we do not store the data, simply return
            return;
        }

        String idempotencyKey = hashedIdempotencyKey.get();
        if (retrieveFromCache(idempotencyKey, now) != null) {
            throw new IdempotencyItemAlreadyExistsException();
        }

        OptionalLong inProgressExpirationMsTimestamp = OptionalLong.empty();
        if (remainingTimeInMs.isPresent()) {
            inProgressExpirationMsTimestamp =
                    OptionalLong.of(now.plus(remainingTimeInMs.getAsInt(), ChronoUnit.MILLIS).toEpochMilli());
        }

        DataRecord dataRecord = new DataRecord(
                idempotencyKey,
                DataRecord.Status.INPROGRESS,
                getExpiryEpochSecond(now),
                null,
                getHashedPayload(data),
                inProgressExpirationMsTimestamp
        );
        LOG.debug("saving in progress record for idempotency key: {}", dataRecord.getIdempotencyKey());
        putRecord(dataRecord, now);
    }

    /**
     * Delete record from the persistence store
     *
     * @param data      Payload
     * @param throwable The throwable thrown by the function
     */
    public void deleteRecord(JsonNode data, Throwable throwable) {
        Optional<String> hashedIdempotencyKey = getHashedIdempotencyKey(data);
        if (!hashedIdempotencyKey.isPresent()) {
            // missing idempotency key => non-idempotent transaction, we do not delete the data, simply return
            return;
        }

        String idemPotencyKey = hashedIdempotencyKey.get();
        LOG.debug("Function raised an exception {}. " +
                        "Clearing in progress record in persistence store for idempotency key: {}",
                throwable.getClass(),
                idemPotencyKey);

        deleteRecord(idemPotencyKey);
        deleteFromCache(idemPotencyKey);
    }

    /**
     * Retrieve idempotency key for data provided, fetch from persistence store, and convert to DataRecord.
     *
     * @param data Payload
     * @return DataRecord representation of existing record found in persistence store
     * @throws IdempotencyValidationException   Payload doesn't match the stored record for the given idempotency key
     * @throws IdempotencyItemNotFoundException Exception thrown if no record exists in persistence store with the idempotency key
     */
    public DataRecord getRecord(JsonNode data, Instant now)
            throws IdempotencyValidationException, IdempotencyItemNotFoundException {
        Optional<String> hashedIdempotencyKey = getHashedIdempotencyKey(data);
        if (!hashedIdempotencyKey.isPresent()) {
            // missing idempotency key => non-idempotent transaction, we do not get the data, simply return nothing
            return null;
        }

        String idemPotencyKey = hashedIdempotencyKey.get();
        DataRecord cachedRecord = retrieveFromCache(idemPotencyKey, now);
        if (cachedRecord != null) {
            LOG.debug("Idempotency record found in cache with idempotency key: {}", idemPotencyKey);
            validatePayload(data, cachedRecord);
            return cachedRecord;
        }

        DataRecord dataRecord = getRecord(idemPotencyKey);
        saveToCache(dataRecord);
        validatePayload(data, dataRecord);
        return dataRecord;
    }

    /**
     * Extract idempotency key and return a hashed representation
     *
     * @param data incoming data
     * @return Hashed representation of the data extracted by the jmespath expression
     */
    private Optional<String> getHashedIdempotencyKey(JsonNode data) {
        JsonNode node = data;

        if (eventKeyJMESPath != null) {
            node = eventKeyCompiledJMESPath.search(data);
        }

        if (isMissingIdemPotencyKey(node)) {
            if (throwOnNoIdempotencyKey) {
                throw new IdempotencyKeyException("No data found to create a hashed idempotency key");
            } else {
                LOG.warn("No data found to create a hashed idempotency key. JMESPath: {}", eventKeyJMESPath);
                return Optional.empty();
            }
        }

        String hash = generateHash(node);
        hash = functionName + "#" + hash;
        return Optional.of(hash);
    }

    private boolean isMissingIdemPotencyKey(JsonNode data) {
        if (data.isContainerNode()) {
            Stream<JsonNode> stream =
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(data.elements(), Spliterator.ORDERED),
                            false);
            return stream.allMatch(JsonNode::isNull);
        }
        return data.isNull();
    }

    /**
     * Extract payload using validation key jmespath and return a hashed representation
     *
     * @param data Payload
     * @return Hashed representation of the data extracted by the jmespath expression
     */
    private String getHashedPayload(JsonNode data) {
        if (!payloadValidationEnabled) {
            return "";
        }
        JsonNode object = validationKeyJMESPath.search(data);
        return generateHash(object);
    }

    /**
     * Generate a hash value from the provided data
     *
     * @param data data to hash
     * @return Hashed representation of the provided data
     */
    String generateHash(JsonNode data) {
        Object node;
        // if array or object, use the json string representation, otherwise get the real value
        if (data.isContainerNode()) {
            node = data.toString();
        } else if (data.isTextual()) {
            node = data.asText();
        } else if (data.isInt()) {
            node = data.asInt();
        } else if (data.isLong()) {
            node = data.asLong();
        } else if (data.isDouble()) {
            node = data.asDouble();
        } else if (data.isFloat()) {
            node = data.floatValue();
        } else if (data.isBigInteger()) {
            node = data.bigIntegerValue();
        } else if (data.isBigDecimal()) {
            node = data.decimalValue();
        } else if (data.isBoolean()) {
            node = data.asBoolean();
        } else {
            node = data; // anything else
        }

        MessageDigest hashAlgorithm = getHashAlgorithm();
        byte[] digest = hashAlgorithm.digest(node.toString().getBytes(StandardCharsets.UTF_8));
        return String.format("%032x", new BigInteger(1, digest));
    }

    private MessageDigest getHashAlgorithm() {
        MessageDigest hashAlgorithm;
        try {
            hashAlgorithm = MessageDigest.getInstance(hashFunctionName);
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("Error instantiating {} hash function, trying with MD5", hashFunctionName);
            try {
                hashAlgorithm = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("Unable to instantiate MD5 digest", ex);
            }
        }
        return hashAlgorithm;
    }

    /**
     * Validate that the hashed payload matches data provided and stored data record
     *
     * @param data       Payload
     * @param dataRecord DataRecord instance
     */
    private void validatePayload(JsonNode data, DataRecord dataRecord) throws IdempotencyValidationException {
        if (payloadValidationEnabled) {
            String dataHash = getHashedPayload(data);
            if (!isEqual(dataRecord.getPayloadHash(), dataHash)) {
                throw new IdempotencyValidationException("Payload does not match stored record for this event key");
            }
        }
    }

    /**
     * @param now
     * @return unix timestamp of expiry date for idempotency record
     */
    private long getExpiryEpochSecond(Instant now) {
        return now.plus(expirationInSeconds, ChronoUnit.SECONDS).getEpochSecond();
    }

    /**
     * Save data_record to local cache except when status is "INPROGRESS"
     * <br/>
     * NOTE: We can't cache "INPROGRESS" records as we have no way to reflect updates that can happen outside of the
     * execution environment
     *
     * @param dataRecord DataRecord to save in cache
     */
    private void saveToCache(DataRecord dataRecord) {
        if (!useLocalCache) {
            return;
        }
        if (dataRecord.getStatus().equals(DataRecord.Status.INPROGRESS)) {
            return;
        }

        cache.put(dataRecord.getIdempotencyKey(), dataRecord);
    }

    private DataRecord retrieveFromCache(String idempotencyKey, Instant now) {
        if (!useLocalCache) {
            return null;
        }

        DataRecord dataRecord = cache.get(idempotencyKey);
        if (dataRecord != null) {
            if (!dataRecord.isExpired(now)) {
                return dataRecord;
            }
            LOG.debug("Removing expired local cache record for idempotency key: {}", idempotencyKey);
            deleteFromCache(idempotencyKey);
        }
        return null;
    }

    private void deleteFromCache(String idempotencyKey) {
        if (!useLocalCache) {
            return;
        }
        cache.remove(idempotencyKey);
    }

    /**
     * For test purpose only (adding a cache to mock)
     */
    void configure(IdempotencyConfig config, String functionName, LRUCache<String, DataRecord> cache) {
        this.configure(config, functionName);
        this.cache = cache;
    }

    private static boolean isEqual(String dataRecordPayload, String dataHash) {
        if (dataHash != null && dataRecordPayload != null) {
            return dataHash.length() != dataRecordPayload.length() ? false : dataHash.equals(dataRecordPayload);
        } else {
            return false;
        }
    }
}
