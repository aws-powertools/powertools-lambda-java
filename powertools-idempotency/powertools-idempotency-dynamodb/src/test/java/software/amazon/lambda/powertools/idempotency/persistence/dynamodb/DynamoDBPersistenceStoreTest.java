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

package software.amazon.lambda.powertools.idempotency.persistence.dynamodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.lambda.powertools.idempotency.Constants;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for DynamoDBPersistenceStore using mocked DynamoDB client.
 */
public class DynamoDBPersistenceStoreTest {
    protected static final String TABLE_NAME = "idempotency_table";
    private DynamoDBPersistenceStore dynamoDBPersistenceStore;
    
    @Mock
    private DynamoDbClient client;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        dynamoDBPersistenceStore = DynamoDBPersistenceStore.builder()
                .withTableName(TABLE_NAME)
                .withDynamoDbClient(client)
                .build();
    }

    // =================================================================
    //<editor-fold desc="putRecord">
    @Test
    public void putRecord_shouldCreateRecordInDynamoDB() throws IdempotencyItemAlreadyExistsException {
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        
        when(client.putItem(any(PutItemRequest.class))).thenReturn(null);
        
        dynamoDBPersistenceStore.putRecord(new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null), now);

        verify(client, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void putRecord_shouldThrowIdempotencyItemAlreadyExistsException_IfRecordAlreadyExist() {
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();

        Map<String, AttributeValue> existingItem = new HashMap<>();
        existingItem.put("id", AttributeValue.builder().s("key").build());
        existingItem.put("status", AttributeValue.builder().s("COMPLETED").build());
        existingItem.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());

        ConditionalCheckFailedException exception = ConditionalCheckFailedException.builder()
                .item(existingItem)
                .build();
        
        doThrow(exception).when(client).putItem(any(PutItemRequest.class));

        assertThatThrownBy(() -> dynamoDBPersistenceStore.putRecord(
                new DataRecord("key",
                        DataRecord.Status.INPROGRESS,
                        expiry,
                        null,
                        null),
                now)).isInstanceOf(IdempotencyItemAlreadyExistsException.class);
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="getRecord">

    @Test
    public void getRecord_shouldReturnExistingRecord() throws IdempotencyItemNotFoundException {
        Instant now = Instant.now();
        long expiry = now.plus(30, ChronoUnit.SECONDS).getEpochSecond();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("key").build());
        item.put("status", AttributeValue.builder().s("COMPLETED").build());
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("data", AttributeValue.builder().s("Fake Data").build());
        
        GetItemResponse response = GetItemResponse.builder().item(item).build();
        when(client.getItem(any(GetItemRequest.class))).thenReturn(response);

        DataRecord record = dynamoDBPersistenceStore.getRecord("key");

        assertThat(record.getIdempotencyKey()).isEqualTo("key");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getResponseData()).isEqualTo("Fake Data");
        assertThat(record.getExpiryTimestamp()).isEqualTo(expiry);
    }

    @Test
    public void getRecord_shouldThrowException_whenRecordIsAbsent() {
        GetItemResponse response = GetItemResponse.builder().build();
        when(client.getItem(any(GetItemRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> dynamoDBPersistenceStore.getRecord("key")).isInstanceOf(
                IdempotencyItemNotFoundException.class);
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="updateRecord">

    @Test
    public void updateRecord_shouldUpdateRecord() {
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        
        when(client.updateItem(any(UpdateItemRequest.class))).thenReturn(null);
        
        dynamoDBPersistenceStore.configure(IdempotencyConfig.builder().withPayloadValidationJMESPath("path").build(),
                null);

        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, "Fake result", "hash");
        dynamoDBPersistenceStore.updateRecord(record);

        verify(client, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    //</editor-fold>
    // =================================================================

    // =================================================================
    //<editor-fold desc="deleteRecord">

    @Test
    public void deleteRecord_shouldDeleteRecord() {
        when(client.deleteItem(any(DeleteItemRequest.class))).thenReturn(null);

        dynamoDBPersistenceStore.deleteRecord("key");

        verify(client, times(1)).deleteItem(any(DeleteItemRequest.class));
    }

    //</editor-fold>
    // =================================================================

    @Test
    @SetEnvironmentVariable(key = Constants.IDEMPOTENCY_DISABLED_ENV, value = "true")
    public void idempotencyDisabled_noClientShouldBeCreated() {
        DynamoDBPersistenceStore store = DynamoDBPersistenceStore.builder().withTableName(TABLE_NAME).build();
        assertThatThrownBy(() -> store.getRecord("fake")).isInstanceOf(NullPointerException.class);
    }
}