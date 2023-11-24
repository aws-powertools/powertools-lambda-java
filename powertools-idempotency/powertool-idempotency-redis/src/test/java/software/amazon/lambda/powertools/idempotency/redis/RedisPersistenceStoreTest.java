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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
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
    public void putRecord_shouldCreateItemInRedis() {
        Instant now = Instant.now();
        long ttl = 3600;
        long expiry = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        redisPersistenceStore.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        Map<String, String> entry = jedisPool.hgetAll("idempotency:id:key");
        long ttlInRedis = jedisPool.ttl("idempotency:id:key");

        assertThat(entry).isNotNull();
        assertThat(entry.get("status")).isEqualTo("COMPLETED");
        assertThat(entry.get("expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    public void putRecord_shouldCreateItemInRedis_withExistingJedisClient() {
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        RedisPersistenceStore store = new RedisPersistenceStore.Builder().withJedisPooled(jedisPool).build();
        store.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        Map<String, String> entry = jedisPool.hgetAll("idempotency:id:key");

        assertThat(entry).isNotNull();
        assertThat(entry.get("status")).isEqualTo("COMPLETED");
        assertThat(entry.get("expiration")).isEqualTo(String.valueOf(expiry));
    }

    @Test
    public void putRecord_shouldCreateItemInRedis_IfPreviousExpired() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.minus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", String.valueOf(expiry));
        item.put("status", DataRecord.Status.COMPLETED.toString());
        item.put("data", "Fake Data");

        long ttl = 3600;
        long expiry2 = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        jedisPool.hset("idempotency:id:key", item);
        redisPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now);

        Map<String, String> entry = jedisPool.hgetAll("idempotency:id:key");
        long ttlInRedis = jedisPool.ttl("idempotency:id:key");

        assertThat(entry).isNotNull();
        assertThat(entry.get("status")).isEqualTo("INPROGRESS");
        assertThat(entry.get("expiration")).isEqualTo(String.valueOf(expiry2));
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    public void putRecord_shouldCreateItemInRedis_IfLambdaWasInProgressAndTimedOut() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        long progressExpiry = now.minus(30, ChronoUnit.SECONDS).toEpochMilli();
        item.put("expiration", String.valueOf(expiry));
        item.put("status", DataRecord.Status.INPROGRESS.toString());
        item.put("data", "Fake Data");
        item.put("in-progress-expiration", String.valueOf(progressExpiry));
        jedisPool.hset("idempotency:id:key", item);

        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        redisPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now);

        Map<String, String> entry = jedisPool.hgetAll("idempotency:id:key");

        assertThat(entry).isNotNull();
        assertThat(entry.get("status")).isEqualTo("INPROGRESS");
        assertThat(entry.get("expiration")).isEqualTo(String.valueOf(expiry2));
    }

    @Test
    public void putRecord_shouldThrowIdempotencyItemAlreadyExistsException_IfRecordAlreadyExist() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", String.valueOf(expiry)); // not expired
        item.put("status", DataRecord.Status.COMPLETED.toString());
        item.put("data", "Fake Data");

        jedisPool.hset("idempotency:id:key", item);

        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        assertThatThrownBy(() -> redisPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now)
        ).isInstanceOf(IdempotencyItemAlreadyExistsException.class);

        Map<String, String> entry = jedisPool.hgetAll("idempotency:id:key");

        assertThat(entry).isNotNull();
        assertThat(entry.get("status")).isEqualTo("COMPLETED");
        assertThat(entry.get("expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(entry.get("data")).isEqualTo("Fake Data");
    }

    @Test
    public void putRecord_shouldBlockUpdate_IfRecordAlreadyExistAndProgressNotExpiredAfterLambdaTimedOut() {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond(); // not expired
        long progressExpiry = now.plus(30, ChronoUnit.SECONDS).toEpochMilli(); // not expired
        item.put("expiration", String.valueOf(expiry));
        item.put("status", DataRecord.Status.INPROGRESS.toString());
        item.put("data", "Fake Data");
        item.put("in-progress-expiration", String.valueOf(progressExpiry));
        jedisPool.hset("idempotency:id:key", item);

        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        assertThatThrownBy(() -> redisPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        "Fake Data 2",
                        null
                ), now))
                .isInstanceOf(IdempotencyItemAlreadyExistsException.class);

        Map<String, String> entry = jedisPool.hgetAll("idempotency:id:key");

        assertThat(entry).isNotNull();
        assertThat(entry.get("status")).isEqualTo("INPROGRESS");
        assertThat(entry.get("expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(entry.get("data")).isEqualTo("Fake Data");
    }

    @Test
    public void getRecord_shouldReturnExistingRecord() throws IdempotencyItemNotFoundException {

        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", String.valueOf(expiry));
        item.put("status", DataRecord.Status.COMPLETED.toString());
        item.put("data", ("Fake Data"));
        jedisPool.hset("idempotency:id:key", item);

        DataRecord record = redisPersistenceStore.getRecord("key");

        assertThat(record.getIdempotencyKey()).isEqualTo("key");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getResponseData()).isEqualTo("Fake Data");
        assertThat(record.getExpiryTimestamp()).isEqualTo(expiry);
    }

    @Test
    public void getRecord_shouldThrowException_whenRecordIsAbsent() {
        assertThatThrownBy(() -> redisPersistenceStore.getRecord("key")).isInstanceOf(
                IdempotencyItemNotFoundException.class);
    }

    @Test
    public void updateRecord_shouldUpdateRecord() {
        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(360, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", String.valueOf(expiry));
        item.put("status", DataRecord.Status.INPROGRESS.toString());
        jedisPool.hset("idempotency:id:key", item);
        // enable payload validation
        redisPersistenceStore.configure(IdempotencyConfig.builder().withPayloadValidationJMESPath("path").build(),
                null);

        long ttl = 3600;
        expiry = now.plus(ttl, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, "Fake result", "hash");
        redisPersistenceStore.updateRecord(record);

        Map<String, String> itemInDb = jedisPool.hgetAll("idempotency:id:key");
        long ttlInRedis = jedisPool.ttl("idempotency:id:key");

        assertThat(itemInDb.get("status")).isEqualTo("COMPLETED");
        assertThat(itemInDb.get("expiration")).isEqualTo(String.valueOf(expiry));
        assertThat(itemInDb.get("data")).isEqualTo("Fake result");
        assertThat(itemInDb.get("validation")).isEqualTo("hash");
        assertThat(Math.round(ttlInRedis / 100.0) * 100).isEqualTo(ttl);
    }

    @Test
    public void deleteRecord_shouldDeleteRecord() {
        Map<String, String> item = new HashMap<>();
        Instant now = Instant.now();
        long expiry = now.plus(360, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", String.valueOf(expiry));
        item.put("status", DataRecord.Status.INPROGRESS.toString());
        jedisPool.hset("idempotency:id:key", item);

        redisPersistenceStore.deleteRecord("key");

        Map<String, String> items = jedisPool.hgetAll("idempotency:id:key");

        assertThat(items.isEmpty()).isTrue();
    }


    @Test
    public void endToEndWithCustomAttrNamesAndSortKey() throws IdempotencyItemNotFoundException {
        try {
            RedisPersistenceStore persistenceStore = RedisPersistenceStore.builder()
                    .withKeyPrefixName("items-idempotency")
                    .withJedisPooled(jedisPool)
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

            Map<String, String> itemInDb = jedisPool.hgetAll("items-idempotency:key:mykey");

            // GET
            DataRecord recordInDb = persistenceStore.getRecord("mykey");

            assertThat(itemInDb).isNotNull();
            assertThat(itemInDb.get("state")).isEqualTo(recordInDb.getStatus().toString());
            assertThat(itemInDb.get("expiry")).isEqualTo(String.valueOf(recordInDb.getExpiryTimestamp()));

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
            assertThat(jedisPool.hgetAll("items-idempotency:key:mykey").size()).isEqualTo(0);

        } finally {
            try {
                jedisPool.del("items-idempotency:key:mykey");
            } catch (Exception e) {
                // OK
            }
        }
    }

    @Test
    @SetEnvironmentVariable(key = software.amazon.lambda.powertools.idempotency.Constants.IDEMPOTENCY_DISABLED_ENV, value = "true")
    public void idempotencyDisabled_noClientShouldBeCreated() {
        RedisPersistenceStore store = RedisPersistenceStore.builder().build();
        assertThatThrownBy(() -> store.getRecord("key")).isInstanceOf(NullPointerException.class);
    }

    @AfterEach
    public void emptyDB() {
        jedisPool.del("idempotency:id:key");
    }

}
