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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.lambda.powertools.idempotency.Constants;
import software.amazon.lambda.powertools.idempotency.DynamoDBConfig;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * These test are using DynamoDBLocal and sqlite, see https://nickolasfisher.com/blog/Configuring-an-In-Memory-DynamoDB-instance-with-Java-for-Integration-Testing
 * NOTE: on a Mac with Apple Chipset, you need to use the Oracle JDK x86 64-bit
 */
public class DynamoDBPersistenceStoreTest extends DynamoDBConfig {
    protected static final String TABLE_NAME_CUSTOM = "idempotency_table_custom";
    private Map<String, AttributeValue> key;
    private DynamoDBPersistenceStore dynamoDBPersistenceStore;

    // =================================================================
    //<editor-fold desc="putRecord">
    @Test
    public void putRecord_shouldCreateRecordInDynamoDB() throws IdempotencyItemAlreadyExistsException {
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        dynamoDBPersistenceStore.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());
        Map<String, AttributeValue> item = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()).item();
        assertThat(item).isNotNull();
        assertThat(item.get("status").s()).isEqualTo("COMPLETED");
        assertThat(item.get("expiration").n()).isEqualTo(String.valueOf(expiry));
    }

    @Test
    public void putRecord_shouldCreateRecordInDynamoDB_IfPreviousExpired() {
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());

        // GIVEN: Insert a fake item with same id and expired
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.minus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("status", AttributeValue.builder().s(DataRecord.Status.COMPLETED.toString()).build());
        item.put("data", AttributeValue.builder().s("Fake Data").build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());

        // WHEN: call putRecord
        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        dynamoDBPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now);

        // THEN: an item is inserted
        Map<String, AttributeValue> itemInDb = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()).item();
        assertThat(itemInDb).isNotNull();
        assertThat(itemInDb.get("status").s()).isEqualTo("INPROGRESS");
        assertThat(itemInDb.get("expiration").n()).isEqualTo(String.valueOf(expiry2));
    }

    @Test
    public void putRecord_shouldCreateRecordInDynamoDB_IfLambdaWasInProgressAndTimedOut() {
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());

        // GIVEN: Insert a fake item with same id and progress expired (Lambda timed out before and we allow a new execution)
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        long progressExpiry = now.minus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("status", AttributeValue.builder().s(DataRecord.Status.INPROGRESS.toString()).build());
        item.put("data", AttributeValue.builder().s("Fake Data").build());
        item.put("in_progress_expiration", AttributeValue.builder().n(String.valueOf(progressExpiry)).build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());

        // WHEN: call putRecord
        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        dynamoDBPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now);

        // THEN: an item is inserted
        Map<String, AttributeValue> itemInDb = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()).item();
        assertThat(itemInDb).isNotNull();
        assertThat(itemInDb.get("status").s()).isEqualTo("INPROGRESS");
        assertThat(itemInDb.get("expiration").n()).isEqualTo(String.valueOf(expiry2));
    }

    @Test
    public void putRecord_shouldThrowIdempotencyItemAlreadyExistsException_IfRecordAlreadyExist() {
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());

        // GIVEN: Insert a fake item with same id
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build()); // not expired
        item.put("status", AttributeValue.builder().s(DataRecord.Status.COMPLETED.toString()).build());
        item.put("data", AttributeValue.builder().s("Fake Data").build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());

        // WHEN: call putRecord
        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        assertThatThrownBy(() -> dynamoDBPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now)
        ).isInstanceOf(IdempotencyItemAlreadyExistsException.class);

        // THEN: item was not updated, retrieve the initial one
        Map<String, AttributeValue> itemInDb = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()).item();
        assertThat(itemInDb).isNotNull();
        assertThat(itemInDb.get("status").s()).isEqualTo("COMPLETED");
        assertThat(itemInDb.get("expiration").n()).isEqualTo(String.valueOf(expiry));
        assertThat(itemInDb.get("data").s()).isEqualTo("Fake Data");
    }

    @Test
    public void putRecord_shouldThrowIdempotencyItemAlreadyExistsException_IfRecordAlreadyExistAndProgressNotExpiredAfterLambdaTimedOut() {
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());

        // GIVEN: Insert a fake item with same id
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond(); // not expired
        long progressExpiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond(); // not expired
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("status", AttributeValue.builder().s(DataRecord.Status.INPROGRESS.toString()).build());
        item.put("data", AttributeValue.builder().s("Fake Data").build());
        item.put("in_progress_expiration", AttributeValue.builder().n(String.valueOf(progressExpiry)).build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());

        // WHEN: call putRecord
        long expiry2 = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        assertThatThrownBy(() -> dynamoDBPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry2,
                        null,
                        null
                ), now)
        ).isInstanceOf(IdempotencyItemAlreadyExistsException.class);

        // THEN: item was not updated, retrieve the initial one
        Map<String, AttributeValue> itemInDb = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()).item();
        assertThat(itemInDb).isNotNull();
        assertThat(itemInDb.get("status").s()).isEqualTo("INPROGRESS");
        assertThat(itemInDb.get("expiration").n()).isEqualTo(String.valueOf(expiry));
        assertThat(itemInDb.get("data").s()).isEqualTo("Fake Data");
    }


    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="getRecord">

    @Test
    public void getRecord_shouldReturnExistingRecord() throws IdempotencyItemNotFoundException {
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());

        // GIVEN: Insert a fake item with same id
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("status", AttributeValue.builder().s(DataRecord.Status.COMPLETED.toString()).build());
        item.put("data", AttributeValue.builder().s("Fake Data").build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());

        // WHEN
        DataRecord record = dynamoDBPersistenceStore.getRecord("key");

        // THEN
        assertThat(record.getIdempotencyKey()).isEqualTo("key");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getResponseData()).isEqualTo("Fake Data");
        assertThat(record.getExpiryTimestamp()).isEqualTo(expiry);
    }

    @Test
    public void getRecord_shouldThrowException_whenRecordIsAbsent() {
        assertThatThrownBy(() -> dynamoDBPersistenceStore.getRecord("key")).isInstanceOf(IdempotencyItemNotFoundException.class);
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="updateRecord">

    @Test
    public void updateRecord_shouldUpdateRecord() {
        // GIVEN: Insert a fake item with same id
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.plus(360, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("status", AttributeValue.builder().s(DataRecord.Status.INPROGRESS.toString()).build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
        // enable payload validation
        dynamoDBPersistenceStore.configure(IdempotencyConfig.builder().withPayloadValidationJMESPath("path").build(), null);

        // WHEN
        expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, "Fake result", "hash");
        dynamoDBPersistenceStore.updateRecord(record);

        // THEN
        Map<String, AttributeValue> itemInDb = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()).item();
        assertThat(itemInDb.get("status").s()).isEqualTo("COMPLETED");
        assertThat(itemInDb.get("expiration").n()).isEqualTo(String.valueOf(expiry));
        assertThat(itemInDb.get("data").s()).isEqualTo("Fake result");
        assertThat(itemInDb.get("validation").s()).isEqualTo("hash");
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="deleteRecord">

    @Test
    public void deleteRecord_shouldDeleteRecord() {
        // GIVEN: Insert a fake item with same id
        key = Collections.singletonMap("id", AttributeValue.builder().s("key").build());
        Map<String, AttributeValue> item = new HashMap<>(key);
        Instant now = Instant.now();
        long expiry = now.plus(360, ChronoUnit.SECONDS).getEpochSecond();
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("status", AttributeValue.builder().s(DataRecord.Status.INPROGRESS.toString()).build());
        client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
        assertThat(client.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).count()).isEqualTo(1);

        // WHEN
        dynamoDBPersistenceStore.deleteRecord("key");

        // THEN
        assertThat(client.scan(ScanRequest.builder().tableName(TABLE_NAME).build()).count()).isEqualTo(0);
    }

    //</editor-fold>
    // =================================================================

    @Test
    public void endToEndWithCustomAttrNamesAndSortKey() throws IdempotencyItemNotFoundException {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(TABLE_NAME_CUSTOM)
                    .keySchema(
                            KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("key").build(),
                            KeySchemaElement.builder().keyType(KeyType.RANGE).attributeName("sortkey").build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("key").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("sortkey").attributeType(ScalarAttributeType.S).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());

            DynamoDBPersistenceStore persistenceStore = DynamoDBPersistenceStore.builder()
                    .withTableName(TABLE_NAME_CUSTOM)
                    .withDynamoDbClient(client)
                    .withDataAttr("result")
                    .withExpiryAttr("expiry")
                    .withKeyAttr("key")
                    .withSortKeyAttr("sortkey")
                    .withStaticPkValue("pk")
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

            Map<String, AttributeValue> customKey = new HashMap<>();
            customKey.put("key", AttributeValue.builder().s("pk").build());
            customKey.put("sortkey", AttributeValue.builder().s("mykey").build());

            Map<String, AttributeValue> itemInDb = client.getItem(GetItemRequest.builder().tableName(TABLE_NAME_CUSTOM).key(customKey).build()).item();

            // GET
            DataRecord recordInDb = persistenceStore.getRecord("mykey");

            assertThat(itemInDb).isNotNull();
            assertThat(itemInDb.get("key").s()).isEqualTo("pk");
            assertThat(itemInDb.get("sortkey").s()).isEqualTo(recordInDb.getIdempotencyKey());
            assertThat(itemInDb.get("state").s()).isEqualTo(recordInDb.getStatus().toString());
            assertThat(itemInDb.get("expiry").n()).isEqualTo(String.valueOf(recordInDb.getExpiryTimestamp()));

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
            assertThat(client.scan(ScanRequest.builder().tableName(TABLE_NAME_CUSTOM).build()).count()).isEqualTo(0);

        } finally {
            try {
                client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME_CUSTOM).build());
            } catch (Exception e) {
                // OK
            }
        }
    }

    @Test
    @SetEnvironmentVariable(key = Constants.IDEMPOTENCY_DISABLED_ENV, value = "true")
    public void idempotencyDisabled_noClientShouldBeCreated() {
        DynamoDBPersistenceStore store = DynamoDBPersistenceStore.builder().withTableName(TABLE_NAME).build();
        assertThatThrownBy(() -> store.getRecord("fake")).isInstanceOf(NullPointerException.class);
    }

    @BeforeEach
    public void setup() {
        dynamoDBPersistenceStore = DynamoDBPersistenceStore.builder()
                .withTableName(TABLE_NAME)
                .withDynamoDbClient(client)
                .build();
    }

    @AfterEach
    public void emptyDB() {
        if (key != null) {
            client.deleteItem(DeleteItemRequest.builder().tableName(TABLE_NAME).key(key).build());
            key = null;
        }
    }
}
