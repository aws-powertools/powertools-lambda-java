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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;

import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyKeyException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyValidationException;
import software.amazon.lambda.powertools.idempotency.internal.cache.LRUCache;
import software.amazon.lambda.powertools.idempotency.model.Product;
import software.amazon.lambda.powertools.utilities.JsonConfig;

class BasePersistenceStoreTest {

    private DataRecord dr;
    private BasePersistenceStore persistenceStore;
    private int status = 0;
    private String validationHash;

    @BeforeEach
    void setup() {
        validationHash = null;
        dr = null;
        status = -1;
        persistenceStore = new BasePersistenceStore() {
            @Override
            public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {
                status = 0;
                return new DataRecord(idempotencyKey, DataRecord.Status.INPROGRESS,
                        Instant.now().plus(3600, ChronoUnit.SECONDS).getEpochSecond(), "Response", validationHash);
            }

            @Override
            public void putRecord(DataRecord dataRecord, Instant now) throws IdempotencyItemAlreadyExistsException {
                dr = dataRecord;
                status = 1;
            }

            @Override
            public void updateRecord(DataRecord dataRecord) {
                dr = dataRecord;
                status = 2;
            }

            @Override
            public void deleteRecord(String idempotencyKey) {
                dr = null;
                status = 3;
            }
        };
    }

    @Test
    void saveInProgress_defaultConfig() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);

        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                OptionalInt.empty());
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isNull();
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction#8d6a8f173b46479eff55e0997864a514");
        assertThat(dr.getPayloadHash()).isEmpty();
        assertThat(dr.getInProgressExpiryTimestamp()).isEmpty();
        assertThat(status).isEqualTo(1);
    }

    @Test
    void saveInProgress_withRemainingTime() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);

        int lambdaTimeoutMs = 30000;
        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                OptionalInt.of(lambdaTimeoutMs));
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isNull();
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction#8d6a8f173b46479eff55e0997864a514");
        assertThat(dr.getPayloadHash()).isEmpty();
        assertThat(dr.getInProgressExpiryTimestamp().orElse(-1)).isEqualTo(
                now.plus(lambdaTimeoutMs, ChronoUnit.MILLIS).toEpochMilli());
        assertThat(status).isEqualTo(1);
    }

    @Test
    void saveInProgress_jmespath() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .build(), "myfunc");

        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                OptionalInt.empty());
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isNull();
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction.myfunc#2fef178cc82be5ce3da6c5e0466a6182");
        assertThat(dr.getPayloadHash()).isEmpty();
        assertThat(status).isEqualTo(1);
    }

    @Test
    void saveInProgress_jmespath_NotFound_shouldThrowException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("unavailable")
                .withThrowOnNoIdempotencyKey(true) // should throw
                .build(), "");
        Instant now = Instant.now();
        assertThatThrownBy(
                () -> persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                        OptionalInt.empty()))
                .isInstanceOf(IdempotencyKeyException.class)
                .hasMessageContaining("No data found to create a hashed idempotency key");
        assertThat(status).isEqualTo(-1);
    }

    @Test
    void saveInProgress_jmespath_NotFound_shouldNotPersist() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("unavailable")
                .build(), "");
        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                OptionalInt.empty());
        assertThat(dr).isNull();
        assertThat(status).isEqualTo(-1);
    }

    @Test
    void saveInProgress_withLocalCache_NotExpired_ShouldThrowException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(true)
                .withEventKeyJMESPath("powertools_json(body).id")
                .build(), null, cache);
        Instant now = Instant.now();
        cache.put("testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                new DataRecord(
                        "testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                        DataRecord.Status.INPROGRESS,
                        now.plus(3600, ChronoUnit.SECONDS).getEpochSecond(),
                        null, null));
        assertThatThrownBy(
                () -> persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                        OptionalInt.empty()))
                .isInstanceOf(IdempotencyItemAlreadyExistsException.class);
        assertThat(status).isEqualTo(-1);
    }

    @Test
    void saveInProgress_withLocalCache_Expired_ShouldRemoveFromCache() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .withUseLocalCache(true)
                .withExpiration(Duration.of(2, ChronoUnit.SECONDS))
                .build(), null, cache);
        Instant now = Instant.now();
        cache.put("testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                new DataRecord(
                        "testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                        DataRecord.Status.INPROGRESS,
                        now.minus(3, ChronoUnit.SECONDS).getEpochSecond(),
                        null, null));
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now,
                OptionalInt.empty());
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(cache).isEmpty();
        assertThat(status).isEqualTo(1);
    }

    @Test
    void saveSuccess_shouldUpdateRecord() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().build(), null, cache);

        Product product = new Product(34543, "product", 42);
        Instant now = Instant.now();
        persistenceStore.saveSuccess(JsonConfig.get().getObjectMapper().valueToTree(event), product, now);

        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isEqualTo(JsonConfig.get().getObjectMapper().writeValueAsString(product));
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction#8d6a8f173b46479eff55e0997864a514");
        assertThat(dr.getPayloadHash()).isEmpty();
        assertThat(status).isEqualTo(2);
        assertThat(cache).isEmpty();
    }

    @Test
    void saveSuccess_withCacheEnabled_shouldSaveInCache() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(true).build(), null, cache);

        Product product = new Product(34543, "product", 42);
        Instant now = Instant.now();
        persistenceStore.saveSuccess(JsonConfig.get().getObjectMapper().valueToTree(event), product, now);

        assertThat(status).isEqualTo(2);
        assertThat(cache).hasSize(1);
        DataRecord cachedDr = cache.get("testFunction#8d6a8f173b46479eff55e0997864a514");
        assertThat(cachedDr.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(cachedDr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(cachedDr.getResponseData()).isEqualTo(JsonConfig.get().getObjectMapper().writeValueAsString(product));
        assertThat(cachedDr.getIdempotencyKey()).isEqualTo("testFunction#8d6a8f173b46479eff55e0997864a514");
        assertThat(cachedDr.getPayloadHash()).isEmpty();
    }

    @Test
    void getRecord_shouldReturnRecordFromPersistence()
            throws IdempotencyItemNotFoundException, IdempotencyValidationException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().build(), "myfunc", cache);

        Instant now = Instant.now();
        DataRecord freshDr = persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(freshDr.getIdempotencyKey()).isEqualTo("testFunction.myfunc#8d6a8f173b46479eff55e0997864a514");
        assertThat(freshDr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(freshDr.getResponseData()).isEqualTo("Response");
        assertThat(status).isZero();
    }

    @Test
    void getRecord_cacheEnabledNotExpired_shouldReturnRecordFromCache()
            throws IdempotencyItemNotFoundException, IdempotencyValidationException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(true).build(), "myfunc", cache);

        Instant now = Instant.now();
        DataRecord dr1 = new DataRecord(
                "testFunction.myfunc#8d6a8f173b46479eff55e0997864a514",
                DataRecord.Status.COMPLETED,
                now.plus(3600, ChronoUnit.SECONDS).getEpochSecond(),
                "result of the function",
                null);
        cache.put("testFunction.myfunc#8d6a8f173b46479eff55e0997864a514", dr1);

        DataRecord dr2 = persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(dr2.getIdempotencyKey()).isEqualTo("testFunction.myfunc#8d6a8f173b46479eff55e0997864a514");
        assertThat(dr2.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(dr2.getResponseData()).isEqualTo("result of the function");
        assertThat(status).isEqualTo(-1); // getRecord must not be called (retrieve from cache)
    }

    @Test
    void getRecord_cacheEnabledExpired_shouldReturnRecordFromPersistence()
            throws IdempotencyItemNotFoundException, IdempotencyValidationException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(true).build(), "myfunc", cache);

        Instant now = Instant.now();
        DataRecord dr1 = new DataRecord(
                "testFunction.myfunc#8d6a8f173b46479eff55e0997864a514",
                DataRecord.Status.COMPLETED,
                now.minus(3, ChronoUnit.SECONDS).getEpochSecond(),
                "result of the function",
                null);
        cache.put("testFunction.myfunc#8d6a8f173b46479eff55e0997864a514", dr1);

        DataRecord dr2 = persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(dr2.getIdempotencyKey()).isEqualTo("testFunction.myfunc#8d6a8f173b46479eff55e0997864a514");
        assertThat(dr2.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(dr2.getResponseData()).isEqualTo("Response");
        assertThat(status).isZero();
        assertThat(cache).isEmpty();
    }

    @Test
    void getRecord_invalidPayload_shouldThrowValidationException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .withPayloadValidationJMESPath("powertools_json(body).message")
                .build(),
                "myfunc");

        this.validationHash = "different hash"; // "Lambda rocks" ==> 70c24d88041893f7fbab4105b76fd9e1

        assertThatThrownBy(
                () -> persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), Instant.now()))
                .isInstanceOf(IdempotencyValidationException.class);
    }

    @Test
    void deleteRecord_shouldDeleteRecordFromPersistence() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);

        persistenceStore.deleteRecord(JsonConfig.get().getObjectMapper().valueToTree(event), new ArithmeticException());
        assertThat(status).isEqualTo(3);
    }

    @Test
    void deleteRecord_cacheEnabled_shouldDeleteRecordFromCache() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(true).build(), null, cache);

        cache.put("testFunction#8d6a8f173b46479eff55e0997864a514",
                new DataRecord("testFunction#8d6a8f173b46479eff55e0997864a514", DataRecord.Status.COMPLETED, 123, null,
                        null));
        persistenceStore.deleteRecord(JsonConfig.get().getObjectMapper().valueToTree(event), new ArithmeticException());
        assertThat(status).isEqualTo(3);
        assertThat(cache).isEmpty();
    }

    @Test
    void generateHashString_shouldGenerateMd5ofString() {
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);
        String expectedHash = "70c24d88041893f7fbab4105b76fd9e1"; // MD5(Lambda rocks)
        String generatedHash = persistenceStore.generateHash(new TextNode("Lambda rocks"));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }

    @Test
    void generateHashObject_shouldGenerateMd5ofJsonObject() {
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);
        Product product = new Product(42, "Product", 12);
        String expectedHash = "e71c41727848ed68050d82740894c29b"; // MD5({"id":42,"name":"Product","price":12.0})
        String generatedHash = persistenceStore.generateHash(JsonConfig.get().getObjectMapper().valueToTree(product));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }

    @Test
    void generateHashDouble_shouldGenerateMd5ofDouble() {
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);
        String expectedHash = "bb84c94278119c8838649706df4db42b"; // MD5(256.42)
        String generatedHash = persistenceStore.generateHash(new DoubleNode(256.42));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }

    @Test
    void generateHashString_withSha256Algorithm_shouldGenerateSha256ofString() {
        persistenceStore.configure(IdempotencyConfig.builder().withHashFunction("SHA-256").build(), null);
        String expectedHash = "e6139efa88ef3337e901e826e6f327337f414860fb499d9f26eefcff21d719af"; // SHA-256(Lambda
                                                                                                  // rocks)
        String generatedHash = persistenceStore.generateHash(new TextNode("Lambda rocks"));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }

    @Test
    void generateHashString_unknownAlgorithm_shouldGenerateMd5ofString() {
        persistenceStore.configure(IdempotencyConfig.builder().withHashFunction("HASH").build(), null);
        String expectedHash = "70c24d88041893f7fbab4105b76fd9e1"; // MD5(Lambda rocks)
        String generatedHash = persistenceStore.generateHash(new TextNode("Lambda rocks"));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }
}
