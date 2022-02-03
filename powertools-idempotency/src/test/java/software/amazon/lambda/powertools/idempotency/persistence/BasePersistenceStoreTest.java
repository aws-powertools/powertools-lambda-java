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

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyKeyException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyValidationException;
import software.amazon.lambda.powertools.idempotency.model.Product;
import software.amazon.lambda.powertools.utilities.JsonConfig;
import software.amazon.lambda.powertools.utilities.cache.LRUCache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BasePersistenceStoreTest {

    private DataRecord dr;
    private BasePersistenceStore persistenceStore;
    private int status = 0;
    private String validationHash;

    @BeforeEach
    public void setup() {
        validationHash = null;
        dr = null;
        status = -1;
        persistenceStore = new BasePersistenceStore() {
            @Override
            public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {
                status = 0;
                return new DataRecord(idempotencyKey, DataRecord.Status.INPROGRESS, Instant.now().plus(3600, ChronoUnit.SECONDS).getEpochSecond(), "Response", validationHash);
            }

            @Override
            public void putRecord(DataRecord record, Instant now) throws IdempotencyItemAlreadyExistsException {
                dr = record;
                status = 1;
            }

            @Override
            public void updateRecord(DataRecord record) {
                dr = record;
                status = 2;
            }

            @Override
            public void deleteRecord(String idempotencyKey) {
                dr = null;
                status = 3;
            }
        };
    }

    // =================================================================
    //<editor-fold desc="saveInProgress">
    @Test
    public void saveInProgress_defaultConfig() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);

        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isNull();
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(dr.getPayloadHash()).isEqualTo("");
        assertThat(status).isEqualTo(1);
    }

    @Test
    public void saveInProgress_jmespath() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .withUseLocalCache(false)
                .build(), "myfunc");

        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isNull();
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction.myfunc#2fef178cc82be5ce3da6c5e0466a6182");
        assertThat(dr.getPayloadHash()).isEqualTo("");
        assertThat(status).isEqualTo(1);
    }

    @Test
    public void saveInProgress_jmespath_NotFound_shouldThrowException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(false)
                .withEventKeyJMESPath("unavailable")
                .withThrowOnNoIdempotencyKey(true) // should throw
                .build(), "");
        Instant now = Instant.now();
        assertThatThrownBy(() -> persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now))
                .isInstanceOf(IdempotencyKeyException.class)
                .hasMessageContaining("No data found to create a hashed idempotency key");
        assertThat(status).isEqualTo(-1);
    }

    @Test
    public void saveInProgress_jmespath_NotFound_shouldNotThrowException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withUseLocalCache(false)
                .withEventKeyJMESPath("unavailable")
                .build(), "");
        Instant now = Instant.now();
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(status).isEqualTo(1);
    }

    @Test
    public void saveInProgress_withLocalCache_NotExpired_ShouldThrowException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .build(), null, cache);
        Instant now = Instant.now();
        cache.put("testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                new DataRecord(
                        "testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                        DataRecord.Status.INPROGRESS,
                        now.plus(3600, ChronoUnit.SECONDS).getEpochSecond(),
                        null, null)
        );
        assertThatThrownBy(() -> persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now))
                .isInstanceOf(IdempotencyItemAlreadyExistsException.class);
        assertThat(status).isEqualTo(-1);
    }

    @Test
    public void saveInProgress_withLocalCache_Expired_ShouldRemoveFromCache() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .withExpirationInSeconds(2)
                .build(), null, cache);
        Instant now = Instant.now();
        cache.put("testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                new DataRecord(
                        "testFunction#2fef178cc82be5ce3da6c5e0466a6182",
                        DataRecord.Status.INPROGRESS,
                        now.minus(3, ChronoUnit.SECONDS).getEpochSecond(),
                        null, null)
        );
        persistenceStore.saveInProgress(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(cache).isEmpty();
        assertThat(status).isEqualTo(1);
    }
    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="saveSuccess">

    @Test
    public void saveSuccess_shouldUpdateRecord() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().withUseLocalCache(false).build(), null, cache);

        Product product = new Product(34543, "product", 42);
        Instant now = Instant.now();
        persistenceStore.saveSuccess(JsonConfig.get().getObjectMapper().valueToTree(event), product, now);

        assertThat(dr.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(dr.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(dr.getResponseData()).isEqualTo(JsonConfig.get().getObjectMapper().writeValueAsString(product));
        assertThat(dr.getIdempotencyKey()).isEqualTo("testFunction#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(dr.getPayloadHash()).isEqualTo("");
        assertThat(status).isEqualTo(2);
        assertThat(cache).isEmpty();
    }

    @Test
    public void saveSuccess_withCacheEnabled_shouldSaveInCache() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().build(), null, cache);

        Product product = new Product(34543, "product", 42);
        Instant now = Instant.now();
        persistenceStore.saveSuccess(JsonConfig.get().getObjectMapper().valueToTree(event), product, now);

        assertThat(status).isEqualTo(2);
        assertThat(cache).hasSize(1);
        DataRecord record = cache.get("testFunction#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getExpiryTimestamp()).isEqualTo(now.plus(3600, ChronoUnit.SECONDS).getEpochSecond());
        assertThat(record.getResponseData()).isEqualTo(JsonConfig.get().getObjectMapper().writeValueAsString(product));
        assertThat(record.getIdempotencyKey()).isEqualTo("testFunction#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(record.getPayloadHash()).isEqualTo("");
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="getRecord">

    @Test
    public void getRecord_shouldReturnRecordFromPersistence() throws IdempotencyItemNotFoundException, IdempotencyValidationException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().withUseLocalCache(false).build(), "myfunc", cache);

        Instant now = Instant.now();
        DataRecord record = persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(record.getIdempotencyKey()).isEqualTo("testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(record.getResponseData()).isEqualTo("Response");
        assertThat(status).isEqualTo(0);
    }

    @Test
    public void getRecord_cacheEnabledNotExpired_shouldReturnRecordFromCache() throws IdempotencyItemNotFoundException, IdempotencyValidationException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().build(), "myfunc", cache);

        Instant now = Instant.now();
        DataRecord dr = new DataRecord(
                "testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f",
                DataRecord.Status.COMPLETED,
                now.plus(3600, ChronoUnit.SECONDS).getEpochSecond(),
                "result of the function",
                null);
        cache.put("testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f", dr);

        DataRecord record = persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(record.getIdempotencyKey()).isEqualTo("testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getResponseData()).isEqualTo("result of the function");
        assertThat(status).isEqualTo(-1); // getRecord must not be called (retrieve from cache)
    }

    @Test
    public void getRecord_cacheEnabledExpired_shouldReturnRecordFromPersistence() throws IdempotencyItemNotFoundException, IdempotencyValidationException {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().build(), "myfunc", cache);

        Instant now = Instant.now();
        DataRecord dr = new DataRecord(
                "testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f",
                DataRecord.Status.COMPLETED,
                now.minus(3, ChronoUnit.SECONDS).getEpochSecond(),
                "result of the function",
                null);
        cache.put("testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f", dr);

        DataRecord record = persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), now);
        assertThat(record.getIdempotencyKey()).isEqualTo("testFunction.myfunc#47261bd5b456f400f8d191cfb3a7482f");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.INPROGRESS);
        assertThat(record.getResponseData()).isEqualTo("Response");
        assertThat(status).isEqualTo(0);
        assertThat(cache).isEmpty();
    }

    @Test
    public void getRecord_invalidPayload_shouldThrowValidationException() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder()
                .withEventKeyJMESPath("powertools_json(body).id")
                .withPayloadValidationJMESPath("powertools_json(body).message")
                .build(),
                "myfunc");

        this.validationHash = "different hash"; // "Lambda rocks" ==> 70c24d88041893f7fbab4105b76fd9e1

        assertThatThrownBy(() -> persistenceStore.getRecord(JsonConfig.get().getObjectMapper().valueToTree(event), Instant.now()))
                .isInstanceOf(IdempotencyValidationException.class);
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="deleteRecord">

    @Test
    public void deleteRecord_shouldDeleteRecordFromPersistence() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);

        persistenceStore.deleteRecord(JsonConfig.get().getObjectMapper().valueToTree(event), new ArithmeticException());
        assertThat(status).isEqualTo(3);
    }

    @Test
    public void deleteRecord_cacheEnabled_shouldDeleteRecordFromCache() {
        APIGatewayProxyRequestEvent event = EventLoader.loadApiGatewayRestEvent("apigw_event.json");
        LRUCache<String, DataRecord> cache = new LRUCache<>(2);
        persistenceStore.configure(IdempotencyConfig.builder().build(), null, cache);

        cache.put("testFunction#47261bd5b456f400f8d191cfb3a7482f",
                new DataRecord("testFunction#47261bd5b456f400f8d191cfb3a7482f", DataRecord.Status.COMPLETED, 123, null, null));
        persistenceStore.deleteRecord(JsonConfig.get().getObjectMapper().valueToTree(event), new ArithmeticException());
        assertThat(status).isEqualTo(3);
        assertThat(cache).isEmpty();
    }

    //</editor-fold>
    // =================================================================

    @Test
    public void generateHashString_shouldGenerateMd5ofString() {
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);
        String expectedHash = "70c24d88041893f7fbab4105b76fd9e1"; // MD5(Lambda rocks)
        String generatedHash = persistenceStore.generateHash(new TextNode("Lambda rocks"));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }

    @Test
    public void generateHashObject_shouldGenerateMd5ofJsonObject() {
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);
        Product product = new Product(42, "Product", 12);
        String expectedHash = "e71c41727848ed68050d82740894c29b"; // MD5({"id":42,"name":"Product","price":12.0})
        String generatedHash = persistenceStore.generateHash(JsonConfig.get().getObjectMapper().valueToTree(product));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }

    @Test
    public void generateHashDouble_shouldGenerateMd5ofDouble() {
        persistenceStore.configure(IdempotencyConfig.builder().build(), null);
        String expectedHash = "bb84c94278119c8838649706df4db42b"; // MD5(256.42)
        String generatedHash = persistenceStore.generateHash(new DoubleNode(256.42));
        assertThat(generatedHash).isEqualTo(expectedHash);
    }
}
