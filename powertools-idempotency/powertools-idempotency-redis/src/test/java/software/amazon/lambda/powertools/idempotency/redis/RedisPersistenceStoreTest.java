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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.lambda.powertools.idempotency.redis.Constants.REDIS_HOST;
import static software.amazon.lambda.powertools.idempotency.redis.Constants.REDIS_PORT;

import com.github.fppt.jedismock.server.ServiceOptions;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;
import redis.embedded.RedisServer;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;

@SetEnvironmentVariable(key = REDIS_HOST, value = "localhost")
@SetEnvironmentVariable(key = REDIS_PORT, value = "6379")
public class RedisPersistenceStoreTest {
    static RedisServer redisServer;
    private final RedisPersistenceStore redisPersistenceStore = RedisPersistenceStore.builder().build();
    private final JedisPooled jedisPool = new JedisPooled();

    public RedisPersistenceStoreTest() {
    }

    @BeforeAll
    public static void init() {
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @AfterAll
    public static void stop() {
        redisServer.stop();
    }

    @Test
    void putRecord_shouldCreateItemInRedis() {
        Instant now = Instant.now();
        long ttl = 3600;
        long expiry = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        redisPersistenceStore.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        Map<String, String> entry = jedisPool.hgetAll("{idempotency:id:key}");
        long ttlInRedis = jedisPool.ttl("{idempotency:id:key}");

        assertThat(entry).isNotNull();
        assertThat(entry.get("{idempotency:id:key}:status")).isEqualTo("COMPLETED");
        assertThat(entry.get("{idempotency:id:key}:expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    void putRecord_shouldCreateItemInRedisClusterMode() throws IOException {
        com.github.fppt.jedismock.RedisServer redisCluster = com.github.fppt.jedismock.RedisServer
                .newRedisServer()
                .setOptions(ServiceOptions.defaultOptions().withClusterModeEnabled())
                .start();
        Instant now = Instant.now();
        long ttl = 3600;
        long expiry = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        JedisPooled jp = new JedisPooled(redisCluster.getHost(), redisCluster.getBindPort());
        RedisPersistenceStore store = new RedisPersistenceStore.Builder().withJedisClient(jp).build();

        store.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        Map<String, String> entry = jp.hgetAll("{idempotency:id:key}");
        long ttlInRedis = jp.ttl("{idempotency:id:key}");

        assertThat(entry).isNotNull();
        assertThat(entry.get("{idempotency:id:key}:status")).isEqualTo("COMPLETED");
        assertThat(entry.get("{idempotency:id:key}:expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @SetEnvironmentVariable(key = Constants.REDIS_CLUSTER_MODE, value = "true")
    @Test
    void putRecord_JedisClientInstanceOfJedisCluster() throws IOException {
        com.github.fppt.jedismock.RedisServer redisCluster = com.github.fppt.jedismock.RedisServer
                .newRedisServer()
                .setOptions(ServiceOptions.defaultOptions().withClusterModeEnabled())
                .start();
        assertThat(redisPersistenceStore.getJedisClient(redisCluster.getHost(), redisCluster.getBindPort()) instanceof JedisCluster).isTrue();
        redisCluster.stop();
    }

    @SetEnvironmentVariable(key = Constants.REDIS_CLUSTER_MODE, value = "false")
    @Test
    void putRecord_JedisClientInstanceOfJedisPooled() {
        assertThat(redisPersistenceStore.getJedisClient(System.getenv(REDIS_HOST), Integer.parseInt(System.getenv(REDIS_PORT))) instanceof JedisCluster).isFalse();
    }
    @Test
    void putRecord_shouldCreateItemInRedisWithInProgressExpiration() {
        Instant now = Instant.now();
        long ttl = 3600;
        long expiry = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        OptionalLong progressExpiry = OptionalLong.of(now.minus(30, ChronoUnit.SECONDS).toEpochMilli());
        redisPersistenceStore.putRecord(
                new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null, progressExpiry), now);

        Map<String, String> redisItem = jedisPool.hgetAll("{idempotency:id:key}");
        long ttlInRedis = jedisPool.ttl("{idempotency:id:key}");

        assertThat(redisItem).isNotNull();
        assertThat(redisItem).containsEntry("{idempotency:id:key}:status", "COMPLETED");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:expiration", String.valueOf(expiry));
        assertThat(redisItem).containsEntry("{idempotency:id:key}:in-progress-expiration",
                String.valueOf(progressExpiry.getAsLong()));
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    void putRecord_shouldCreateItemInRedis_withExistingJedisClient() {
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        RedisPersistenceStore store = new RedisPersistenceStore.Builder().withJedisClient(jedisPool).build();
        store.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        Map<String, String> redisItem = jedisPool.hgetAll("{idempotency:id:key}");

        assertThat(redisItem).isNotNull();
        assertThat(redisItem).containsEntry("{idempotency:id:key}:status", "COMPLETED");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:expiration", String.valueOf(expiry));
    }

    @Test
    void putRecord_shouldCreateItemInRedis_IfPreviousExpired() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.minus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry));
        item.put("{idempotency:id:key}:status", DataRecord.Status.COMPLETED.toString());
        item.put("{idempotency:id:key}:data", "Fake Data");

        long ttl = 3600;
        long expiry2 = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        jedisPool.hset("{idempotency:id:key}", item);
        redisPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now);

        Map<String, String> redisItem = jedisPool.hgetAll("{idempotency:id:key}");
        long ttlInRedis = jedisPool.ttl("{idempotency:id:key}");

        assertThat(redisItem).isNotNull();
        assertThat(redisItem).containsEntry("{idempotency:id:key}:status", "INPROGRESS");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:expiration", String.valueOf(expiry2));
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    void putRecord_shouldCreateItemInRedis_IfLambdaWasInProgressAndTimedOut() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        long progressExpiry = now.minus(30, ChronoUnit.SECONDS).toEpochMilli();
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry));
        item.put("{idempotency:id:key}:status", DataRecord.Status.INPROGRESS.toString());
        item.put("{idempotency:id:key}:data", "Fake Data");
        item.put("{idempotency:id:key}:in-progress-expiration", String.valueOf(progressExpiry));
        jedisPool.hset("{idempotency:id:key}", item);

        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        redisPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now);

        Map<String, String> redisItem = jedisPool.hgetAll("{idempotency:id:key}");

        assertThat(redisItem).isNotNull();
        assertThat(redisItem).containsEntry("{idempotency:id:key}:status", "INPROGRESS");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:expiration", String.valueOf(expiry2));
    }

    @Test
    void putRecord_shouldThrowIdempotencyItemAlreadyExistsException_IfRecordAlreadyExist() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry)); // not expired
        item.put("{idempotency:id:key}:status", DataRecord.Status.COMPLETED.toString());
        item.put("{idempotency:id:key}:data", "Fake Data");

        jedisPool.hset("{idempotency:id:key}", item);

        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord dataRecord = new DataRecord("key",
                DataRecord.Status.INPROGRESS,
                expiry2,
                null,
                null
        );
        assertThatThrownBy(() -> {
                    redisPersistenceStore.putRecord(
                            dataRecord, now);
                }
        ).isInstanceOf(IdempotencyItemAlreadyExistsException.class);

        Map<String, String> entry = jedisPool.hgetAll("{idempotency:id:key}");

        assertThat(entry).isNotNull();
        assertThat(entry.get("{idempotency:id:key}:status")).isEqualTo("COMPLETED");
        assertThat(entry.get("{idempotency:id:key}:expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(entry.get("{idempotency:id:key}:data")).isEqualTo("Fake Data");
    }

    @Test
    void putRecord_shouldBlockUpdate_IfRecordAlreadyExistAndProgressNotExpiredAfterLambdaTimedOut() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond(); // not expired
        long progressExpiry = now.plus(30, ChronoUnit.SECONDS).toEpochMilli(); // not expired
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry));
        item.put("{idempotency:id:key}:status", DataRecord.Status.INPROGRESS.toString());
        item.put("{idempotency:id:key}:data", "Fake Data");
        item.put("{idempotency:id:key}:in-progress-expiration", String.valueOf(progressExpiry));
        jedisPool.hset("{idempotency:id:key}", item);

        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord dataRecord = new DataRecord("key",
                DataRecord.Status.INPROGRESS,
                expiry2,
                "Fake Data 2",
                null
        );
        assertThatThrownBy(() -> redisPersistenceStore.putRecord(
                dataRecord, now))
                .isInstanceOf(IdempotencyItemAlreadyExistsException.class);

        Map<String, String> redisItem = jedisPool.hgetAll("{idempotency:id:key}");

        assertThat(redisItem).isNotNull();
        assertThat(redisItem).containsEntry("{idempotency:id:key}:status", "INPROGRESS");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:expiration", String.valueOf(expiry));
        assertThat(redisItem).containsEntry("{idempotency:id:key}:data", "Fake Data");
    }

    @Test
    void getRecord_shouldReturnExistingRecord() throws IdempotencyItemNotFoundException {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry));
        item.put("{idempotency:id:key}:status", DataRecord.Status.COMPLETED.toString());
        item.put("{idempotency:id:key}:data", ("Fake Data"));
        jedisPool.hset("{idempotency:id:key}", item);

        DataRecord record = redisPersistenceStore.getRecord("key");

        assertThat(record.getIdempotencyKey()).isEqualTo("key");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getResponseData()).isEqualTo("Fake Data");
        assertThat(record.getExpiryTimestamp()).isEqualTo(expiry);
    }

    @Test
    void getRecord_shouldThrowException_whenRecordIsAbsent() {
        assertThatThrownBy(() -> redisPersistenceStore.getRecord("key")).isInstanceOf(
                IdempotencyItemNotFoundException.class);
    }

    @Test
    void updateRecord_shouldUpdateRecord() {
        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(360, ChronoUnit.SECONDS).getEpochSecond();
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry));
        item.put("{idempotency:id:key}:status", DataRecord.Status.INPROGRESS.toString());
        jedisPool.hset("{idempotency:id:key}", item);
        // enable payload validation
        redisPersistenceStore.configure(IdempotencyConfig.builder().withPayloadValidationJMESPath("path").build(),
                null);

        long ttl = 3600;
        expiry = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, "Fake result", "hash");
        redisPersistenceStore.updateRecord(record);

        Map<String, String> redisItem = jedisPool.hgetAll("{idempotency:id:key}");
        long ttlInRedis = jedisPool.ttl("{idempotency:id:key}");

        assertThat(redisItem).containsEntry("{idempotency:id:key}:status", "COMPLETED");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:expiration", String.valueOf(expiry));
        assertThat(redisItem).containsEntry("{idempotency:id:key}:data", "Fake result");
        assertThat(redisItem).containsEntry("{idempotency:id:key}:validation", "hash");
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    void deleteRecord_shouldDeleteRecord() {
        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(360, ChronoUnit.SECONDS).getEpochSecond();
        item.put("{idempotency:id:key}:expiration", String.valueOf(expiry));
        item.put("{idempotency:id:key}:status", DataRecord.Status.INPROGRESS.toString());
        jedisPool.hset("{idempotency:id:key}", item);

        redisPersistenceStore.deleteRecord("key");

        Map<String, String> items = jedisPool.hgetAll("{idempotency:id:key}");

        assertThat(items).isEmpty();
    }


    @Test
    void endToEndWithCustomAttrNamesAndSortKey() throws IdempotencyItemNotFoundException {
        try {
            RedisPersistenceStore persistenceStore = RedisPersistenceStore.builder()
                    .withKeyPrefixName("items-idempotency")
                    .withJedisClient(jedisPool)
                    .withDataAttr("result")
                    .withExpiryAttr("expiry")
                    .withKeyAttr("key")
                    .withStatusAttr("state")
                    .withValidationAttr("valid")
                    .build();

            Instant now = Instant.now();
            DataRecord record = new DataRecord(
                    "mykey",
                    DataRecord.Status.INPROGRESS,
                    now.plus(400, ChronoUnit.SECONDS).getEpochSecond(),
                    null,
                    null
            );
            // PUT
            persistenceStore.putRecord(record, now);

            Map<String, String> redisItem = jedisPool.hgetAll("{items-idempotency:key:mykey}");

            // GET
            DataRecord recordInDb = persistenceStore.getRecord("mykey");

            assertThat(redisItem).isNotNull();
            assertThat(redisItem).containsEntry("{items-idempotency:key:mykey}:state",
                    recordInDb.getStatus().toString());
            assertThat(redisItem).containsEntry("{items-idempotency:key:mykey}:expiry",
                    String.valueOf(recordInDb.getExpiryTimestamp()));

            // UPDATE
            DataRecord updatedRecord = new DataRecord(
                    "mykey",
                    DataRecord.Status.COMPLETED,
                    now.plus(500, ChronoUnit.SECONDS).getEpochSecond(),
                    "response",
                    null
            );
            persistenceStore.updateRecord(updatedRecord);
            recordInDb = persistenceStore.getRecord("mykey");
            assertThat(recordInDb).isEqualTo(updatedRecord);

            // DELETE
            persistenceStore.deleteRecord("mykey");
            assertThat(jedisPool.hgetAll("{items-idempotency:key:mykey}").size()).isZero();

        } finally {
            jedisPool.del("{items-idempotency:key:mykey}");
        }
    }

    @Test
    @SetEnvironmentVariable(key = software.amazon.lambda.powertools.idempotency.Constants.IDEMPOTENCY_DISABLED_ENV, value = "true")
    void idempotencyDisabled_noClientShouldBeCreated() {
        RedisPersistenceStore store = RedisPersistenceStore.builder().build();
        assertThatThrownBy(() -> store.getRecord("key")).isInstanceOf(NullPointerException.class);
    }

    @AfterEach
    void emptyDB() {
        jedisPool.del("{idempotency:id:key}");
    }

}
