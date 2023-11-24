package software.amazon.lambda.powertools.idempotency.redis;/*
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

import static software.amazon.lambda.powertools.idempotency.persistence.DataRecord.Status.INPROGRESS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.persistence.BasePersistenceStore;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;
import software.amazon.lambda.powertools.idempotency.persistence.PersistenceStore;

/**
 * Redis version of the {@link PersistenceStore}. Will store idempotency data in Redis.<br>
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
    private final JedisPooled jedisPool;

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
                                  JedisPooled jedisPool) {
        this.keyPrefixName = keyPrefixName;
        this.keyAttr = keyAttr;
        this.expiryAttr = expiryAttr;
        this.inProgressExpiryAttr = inProgressExpiryAttr;
        this.statusAttr = statusAttr;
        this.dataAttr = dataAttr;
        this.validationAttr = validationAttr;

        if (jedisPool != null) {
            this.jedisPool = jedisPool;
        } else {
            String idempotencyDisabledEnv = System.getenv().get(software.amazon.lambda.powertools.idempotency.Constants.IDEMPOTENCY_DISABLED_ENV);
            if (idempotencyDisabledEnv == null || "false".equalsIgnoreCase(idempotencyDisabledEnv)) {
                HostAndPort address = new HostAndPort(System.getenv().get(Constants.REDIS_HOST),
                        Integer.parseInt(System.getenv().get(Constants.REDIS_PORT)));
                JedisClientConfig config = getJedisClientConfig();
                this.jedisPool = new JedisPooled(address, config);
            } else {
                // we do not want to create a Jedis connection pool if idempotency is disabled
                // null is ok as idempotency won't be called
                this.jedisPool = null;
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Set redis user and secret to connect to the redis server
     *
     * @return
     */
    private static JedisClientConfig getJedisClientConfig() {
        return DefaultJedisClientConfig.builder()
                .user(System.getenv().get(Constants.REDIS_USER))
                .password(System.getenv().get(Constants.REDIS_SECRET))
                .build();
    }

    JedisClientConfig config = getJedisClientConfig();


    @Override
    public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {

        Map<String, String> item = jedisPool.hgetAll(getKey(idempotencyKey));
        if (item.isEmpty()) {
            throw new IdempotencyItemNotFoundException(idempotencyKey);
        }
        item.put(this.keyAttr, idempotencyKey);
        return itemToRecord(item);
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

        LOG.debug("Putting dataRecord for idempotency key: {}", dataRecord.getIdempotencyKey());

        Object execRes = putItemOnCondition(dataRecord, now, inProgressExpiry);

        if (execRes == null) {
            String msg = String.format("Failed to put dataRecord for already existing idempotency key: %s",
                    getKey(dataRecord.getIdempotencyKey()));
            LOG.debug(msg);
            throw new IdempotencyItemAlreadyExistsException(msg);
        } else {
            LOG.debug("Record for idempotency key is set: {}", dataRecord.getIdempotencyKey());
            jedisPool.expireAt(getKey(dataRecord.getIdempotencyKey()), dataRecord.getExpiryTimestamp());
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
        fields.add(getKey(dataRecord.getIdempotencyKey()));
        fields.add(this.expiryAttr);
        fields.add(this.statusAttr);
        fields.add(this.inProgressExpiryAttr);
        fields.add(String.valueOf(now.getEpochSecond()));
        fields.add(String.valueOf(now.toEpochMilli()));
        fields.add(INPROGRESS.toString());
        fields.add(String.valueOf(dataRecord.getExpiryTimestamp()));
        fields.add(dataRecord.getStatus().toString());

        if (inProgressExpiry != null) {
            fields.add(inProgressExpiry);
        }

        String[] arr = new String[fields.size()];
        return jedisPool.eval(luaScript, 4, (String[]) fields.toArray(arr));
    }

    @Override
    public void updateRecord(DataRecord dataRecord) {
        LOG.debug("Updating dataRecord for idempotency key: {}", dataRecord.getIdempotencyKey());

        Map<String, String> item = new HashMap<>();
        item.put(this.dataAttr, dataRecord.getResponseData());
        item.put(this.expiryAttr, String.valueOf(dataRecord.getExpiryTimestamp()));
        item.put(this.statusAttr, String.valueOf(dataRecord.getStatus().toString()));

        if (payloadValidationEnabled) {
            item.put(this.validationAttr, dataRecord.getPayloadHash());
        }

        jedisPool.hset(getKey(dataRecord.getIdempotencyKey()), item);
        jedisPool.expireAt(getKey(dataRecord.getIdempotencyKey()), dataRecord.getExpiryTimestamp());
    }

    @Override
    public void deleteRecord(String idempotencyKey) {
        LOG.debug("Deleting record for idempotency key: {}", idempotencyKey);
        jedisPool.del(getKey(idempotencyKey));
    }

    /**
     * Get the key to use for requests
     * Sets a keyPrefixName for hash name and a keyAttr for hash key
     *
     * @param idempotencyKey
     * @return
     */
    private String getKey(String idempotencyKey) {
        return this.keyPrefixName + ":" + this.keyAttr + ":" + idempotencyKey;
    }

    /**
     * Translate raw item records from Redis to DataRecord
     *
     * @param item Item from redis response
     * @return DataRecord instance
     */
    private DataRecord itemToRecord(Map<String, String> item) {
        // data and validation payload may be null
        String data = item.get(this.dataAttr);
        String validation = item.get(this.validationAttr);
        return new DataRecord(item.get(keyAttr),
                DataRecord.Status.valueOf(item.get(this.statusAttr)),
                Long.parseLong(item.get(this.expiryAttr)),
                data,
                validation,
                item.get(this.inProgressExpiryAttr) != null ?
                        OptionalLong.of(Long.parseLong(item.get(this.inProgressExpiryAttr))) :
                        OptionalLong.empty());
    }

    /**
     * Use this builder to get an instance of {@link RedisPersistenceStore}.<br/>
     * With this builder you can configure the characteristics of the Redis hash fields.<br/>
     * You can also set a custom {@link JedisPool}.
     */
    public static class Builder {
        private String keyPrefixName = "idempotency";
        private String keyAttr = "id";
        private String expiryAttr = "expiration";
        private String inProgressExpiryAttr = "in-progress-expiration";
        private String statusAttr = "status";
        private String dataAttr = "data";
        private String validationAttr = "validation";
        private JedisPooled jedisPool;

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
         * Custom {@link JedisPool} used to query DynamoDB (optional).<br/>
         *
         * @param jedisPool the {@link JedisPool} instance to use
         * @return the builder instance (to chain operations)
         */
        public Builder withJedisPooled(JedisPooled jedisPool) {
            this.jedisPool = jedisPool;
            return this;
        }
    }
}