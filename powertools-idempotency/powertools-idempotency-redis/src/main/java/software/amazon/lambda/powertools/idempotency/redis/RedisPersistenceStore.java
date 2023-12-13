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

package software.amazon.lambda.powertools.idempotency.redis;

import static software.amazon.lambda.powertools.idempotency.persistence.DataRecord.Status.INPROGRESS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;
import software.amazon.lambda.powertools.idempotency.persistence.PersistenceStore;

/**
 * Redis version of the {@link PersistenceStore}. Stores idempotency data in Redis standalone or cluster mode.<br>
 * Use the {@link Builder} to create a new instance.
 */
public class RedisPersistenceStore extends BasePersistenceStore implements PersistenceStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPersistenceStore.class);
    private final String keyPrefixName;
    private final String keyAttr;
    private final String expiryAttr;
    private final String inProgressExpiryAttr;
    private final String statusAttr;
    private final String dataAttr;
    private final String validationAttr;
    private final UnifiedJedis jedisClient;

    /**
     * Private: use the {@link Builder} to instantiate a new {@link RedisPersistenceStore}
     */
    private RedisPersistenceStore(String keyPrefixName,
                                  String keyAttr,
                                  String expiryAttr,
                                  String inProgressExpiryAttr,
                                  String statusAttr,
                                  String dataAttr,
                                  String validationAttr,
                                  UnifiedJedis jedisClient) {
        this.keyPrefixName = keyPrefixName;
        this.keyAttr = keyAttr;
        this.expiryAttr = expiryAttr;
        this.inProgressExpiryAttr = inProgressExpiryAttr;
        this.statusAttr = statusAttr;
        this.dataAttr = dataAttr;
        this.validationAttr = validationAttr;

        if (jedisClient != null) {
            this.jedisClient = jedisClient;
        } else {
            String idempotencyDisabledEnv =
                    System.getenv(software.amazon.lambda.powertools.idempotency.Constants.IDEMPOTENCY_DISABLED_ENV);
            if (idempotencyDisabledEnv == null || "false".equalsIgnoreCase(idempotencyDisabledEnv)) {
                this.jedisClient = getJedisClient(System.getenv(Constants.REDIS_HOST),
                        Integer.parseInt(System.getenv(Constants.REDIS_PORT)));
            } else {
                // we do not want to create a Jedis connection pool if idempotency is disabled
                // null is ok as idempotency won't be called
                this.jedisClient = null;
            }
        }
    }

    /**
     * Set redis user and secret to connect to the redis server
     *
     * @return
     */
    private static JedisClientConfig getJedisClientConfig() {
        String redisSecret = "";
        String redisSecretEnv = System.getenv(Constants.REDIS_SECRET);
        if (redisSecretEnv != null) {
            redisSecret = redisSecretEnv;
        }
        return DefaultJedisClientConfig.builder()
                .user(System.getenv(Constants.REDIS_USER))
                .password(System.getenv(redisSecret))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    UnifiedJedis getJedisClient(String redisHost, Integer redisPort) {
        HostAndPort address = new HostAndPort(redisHost, redisPort);
        JedisClientConfig config = getJedisClientConfig();
        String isClusterMode = System.getenv(Constants.REDIS_CLUSTER_MODE);
        if ("true".equalsIgnoreCase(isClusterMode)) {
            return new JedisCluster(address, getJedisClientConfig(), 5, new GenericObjectPoolConfig<>());
        } else {
            return new JedisPooled(address, config);
        }
    }

    @Override
    public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {

        String hashKey = getKey(idempotencyKey);
        Map<String, String> item = jedisClient.hgetAll(hashKey);
        if (item.isEmpty()) {
            throw new IdempotencyItemNotFoundException(idempotencyKey);
        }
        item.put(hashKey, idempotencyKey);
        return itemToRecord(item, idempotencyKey);
    }

    /**
     * Store's the given idempotency dataRecord in the redis store. If there
     * is an existing dataRecord that has expired - either due to the
     * cache expiry or due to the in_progress_expiry - the dataRecord
     * will be overwritten and the idempotent operation can continue.
     *
     * <b>Note: This method writes only expiry and status information - not
     * the results of the operation itself.</b>
     *
     * @param dataRecord DataRecord instance to store
     * @param now
     * @throws IdempotencyItemAlreadyExistsException
     */
    @Override
    public void putRecord(DataRecord dataRecord, Instant now) {

        String inProgressExpiry = null;
        if (dataRecord.getInProgressExpiryTimestamp().isPresent()) {
            inProgressExpiry = String.valueOf(dataRecord.getInProgressExpiryTimestamp().getAsLong());
        }

        LOG.info("Putting dataRecord for idempotency key: {}", dataRecord.getIdempotencyKey());

        Object execRes = putItemOnCondition(dataRecord, now, inProgressExpiry);

        if (execRes == null) {
            String msg = String.format("Failed to put dataRecord for already existing idempotency key: %s",
                    getKey(dataRecord.getIdempotencyKey()));
            LOG.info(msg);
            throw new IdempotencyItemAlreadyExistsException(msg);
        } else {
            LOG.info("Record for idempotency key is set: {}", dataRecord.getIdempotencyKey());
            jedisClient.expireAt(getKey(dataRecord.getIdempotencyKey()), dataRecord.getExpiryTimestamp());
        }
    }

    private Object putItemOnCondition(DataRecord dataRecord, Instant now, String inProgressExpiry) {
        // if item with key exists
        String redisHashExistsExpression = "redis.call('exists', KEYS[1]) == 0";
        // if expiry timestamp is exceeded for existing item
        String itemExpiredExpression = "redis.call('hget', KEYS[1], KEYS[2]) < ARGV[1]";
        // if item status field exists and has value is INPROGRESS
        // and the in-progress-expiry timestamp is still valid
        String itemIsInProgressExpression = "(redis.call('hexists', KEYS[1], KEYS[4]) ~= 0" +
                " and redis.call('hget', KEYS[1], KEYS[4]) < ARGV[2]" +
                " and redis.call('hget', KEYS[1], KEYS[3]) == ARGV[3])";

        // insert item and fields
        String insertItemExpression = "return redis.call('hset', KEYS[1], KEYS[2], ARGV[4], KEYS[3], ARGV[5])";

        // only insert in-progress-expiry if it is set
        if (inProgressExpiry != null) {
            insertItemExpression = insertItemExpression.replace(")", ", KEYS[4], ARGV[6])");
        }

        // if redisHashExistsExpression or itemExpiredExpression or itemIsInProgressExpression then insertItemExpression
        String luaScript = String.format("if %s or %s or %s then %s end;",
                redisHashExistsExpression, itemExpiredExpression,
                itemIsInProgressExpression, insertItemExpression);

        List<String> fields = new ArrayList<>();
        String hashKey = getKey(dataRecord.getIdempotencyKey());
        fields.add(hashKey);
        fields.add(prependField(hashKey, this.expiryAttr));
        fields.add(prependField(hashKey, this.statusAttr));
        fields.add(prependField(hashKey, this.inProgressExpiryAttr));
        fields.add(String.valueOf(now.getEpochSecond()));
        fields.add(String.valueOf(now.toEpochMilli()));
        fields.add(INPROGRESS.toString());
        fields.add(String.valueOf(dataRecord.getExpiryTimestamp()));
        fields.add(dataRecord.getStatus().toString());

        if (inProgressExpiry != null) {
            fields.add(inProgressExpiry);
        }

        String[] arr = new String[fields.size()];
        return jedisClient.eval(luaScript, 4, fields.toArray(arr));
    }

    @Override
    public void updateRecord(DataRecord dataRecord) {
        LOG.debug("Updating dataRecord for idempotency key: {}", dataRecord.getIdempotencyKey());
        String hashKey = getKey(dataRecord.getIdempotencyKey());

        Map<String, String> item = new HashMap<>();
        item.put(prependField(hashKey, this.dataAttr), dataRecord.getResponseData());
        item.put(prependField(hashKey, this.expiryAttr), String.valueOf(dataRecord.getExpiryTimestamp()));
        item.put(prependField(hashKey, this.statusAttr), String.valueOf(dataRecord.getStatus().toString()));

        if (payloadValidationEnabled) {
            item.put(prependField(hashKey, this.validationAttr), dataRecord.getPayloadHash());
        }

        jedisClient.hset(hashKey, item);
        jedisClient.expireAt(hashKey, dataRecord.getExpiryTimestamp());
    }


    @Override
    public void deleteRecord(String idempotencyKey) {
        LOG.debug("Deleting record for idempotency key: {}", idempotencyKey);
        jedisClient.del(getKey(idempotencyKey));
    }

    /**
     * Get the key to use for requests
     * Sets a keyPrefixName for hash name and a keyAttr for hash key
     * The key will be used in multi-key operations, therefore we need to
     * include it into curly braces to instruct the redis cluster which part
     * of the key will be used for hash and should be stored and looked-up in the same slot.
     *
     * @param idempotencyKey
     * @return
     * @see <a href="https://redis.io/docs/reference/cluster-spec/#key-distribution-model">Redis Key distribution model</a>
     */
    private String getKey(String idempotencyKey) {
        return "{" + this.keyPrefixName + ":" + this.keyAttr + ":" + idempotencyKey + "}";
    }

    /**
     * Prepend each field key with the unique prefix that will be used for calculating the hash slot
     * it will be stored in case of cluster mode Redis deployement
     *
     * @param hashKey
     * @param field
     * @return
     * @see <a href="https://redis.io/docs/reference/cluster-spec/#key-distribution-model">Redis Key distribution model</a>
     */
    private String prependField(String hashKey, String field) {
        return hashKey + ":" + field;
    }

    /**
     * Translate raw item records from Redis to DataRecord
     *
     * @param item Item from redis response
     * @return DataRecord instance
     */
    private DataRecord itemToRecord(Map<String, String> item, String idempotencyKey) {
        String hashKey = getKey(idempotencyKey);
        return new DataRecord(item.get(getKey(idempotencyKey)),
                DataRecord.Status.valueOf(item.get(prependField(hashKey, this.statusAttr))),
                Long.parseLong(item.get(prependField(hashKey, this.expiryAttr))),
                item.get(prependField(hashKey, this.dataAttr)),
                item.get(prependField(hashKey, this.validationAttr)),
                item.get(prependField(hashKey, this.inProgressExpiryAttr)) != null ?
                        OptionalLong.of(Long.parseLong(item.get(prependField(hashKey, this.inProgressExpiryAttr)))) :
                        OptionalLong.empty());
    }

    /**
     * Use this builder to get an instance of {@link RedisPersistenceStore}.<br/>
     * With this builder you can configure the characteristics of the Redis hash fields.<br/>
     * You can also set a custom {@link UnifiedJedis} client.
     */
    public static class Builder {
        private String keyPrefixName = "idempotency";
        private String keyAttr = "id";
        private String expiryAttr = "expiration";
        private String inProgressExpiryAttr = "in-progress-expiration";
        private String statusAttr = "status";
        private String dataAttr = "data";
        private String validationAttr = "validation";
        private UnifiedJedis jedisPool;

        /**
         * Initialize and return a new instance of {@link RedisPersistenceStore}.<br/>
         * Example:<br>
         * <pre>
         *     RedisPersistenceStore.builder().withKeyAttr("uuid").build();
         * </pre>
         *
         * @return an instance of the {@link RedisPersistenceStore}
         */
        public RedisPersistenceStore build() {
            return new RedisPersistenceStore(keyPrefixName, keyAttr, expiryAttr,
                    inProgressExpiryAttr, statusAttr, dataAttr, validationAttr, jedisPool);
        }

        /**
         * Redis prefix for the hash key (optional), by default "idempotency"
         *
         * @param keyPrefixName name of the key prefix
         * @return the builder instance (to chain operations)
         */
        public Builder withKeyPrefixName(String keyPrefixName) {
            this.keyPrefixName = keyPrefixName;
            return this;
        }

        /**
         * Redis name for hash key (optional), by default "id"
         *
         * @param keyAttr name of the key field of the hash
         * @return the builder instance (to chain operations)
         */
        public Builder withKeyAttr(String keyAttr) {
            this.keyAttr = keyAttr;
            return this;
        }

        /**
         * Redis field name for expiry timestamp (optional), by default "expiration"
         *
         * @param expiryAttr name of the expiry field in the hash
         * @return the builder instance (to chain operations)
         */
        public Builder withExpiryAttr(String expiryAttr) {
            this.expiryAttr = expiryAttr;
            return this;
        }

        /**
         * Redis field name for in progress expiry timestamp (optional), by default "in-progress-expiration"
         *
         * @param inProgressExpiryAttr name of the field in the hash
         * @return the builder instance (to chain operations)
         */
        public Builder withInProgressExpiryAttr(String inProgressExpiryAttr) {
            this.inProgressExpiryAttr = inProgressExpiryAttr;
            return this;
        }

        /**
         * Redis field name for status (optional), by default "status"
         *
         * @param statusAttr name of the status field in the hash
         * @return the builder instance (to chain operations)
         */
        public Builder withStatusAttr(String statusAttr) {
            this.statusAttr = statusAttr;
            return this;
        }

        /**
         * Redis field name for response data (optional), by default "data"
         *
         * @param dataAttr name of the data field in the hash
         * @return the builder instance (to chain operations)
         */
        public Builder withDataAttr(String dataAttr) {
            this.dataAttr = dataAttr;
            return this;
        }

        /**
         * Redis field name for validation (optional), by default "validation"
         *
         * @param validationAttr name of the validation field in the hash
         * @return the builder instance (to chain operations)
         */
        public Builder withValidationAttr(String validationAttr) {
            this.validationAttr = validationAttr;
            return this;
        }

        /**
         * Custom {@link UnifiedJedis} used to query Redis (optional).<br/>
         * This will be cast to either {@link JedisPool} or {@link JedisCluster}
         * depending on the mode of the Redis deployment and instructed by
         * the value of {@link Constants#REDIS_CLUSTER_MODE} environment variable.<br/>
         *
         * @param jedisClient the {@link UnifiedJedis} instance to use
         * @return the builder instance (to chain operations)
         */
        public Builder withJedisClient(UnifiedJedis jedisClient) {
            this.jedisPool = jedisClient;
            return this;
        }
    }
}
