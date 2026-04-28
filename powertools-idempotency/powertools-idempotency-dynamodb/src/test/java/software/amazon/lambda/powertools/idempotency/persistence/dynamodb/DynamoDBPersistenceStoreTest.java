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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.lambda.powertools.idempotency.Constants;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;
import software.amazon.lambda.powertools.idempotency.persistence.DataRecord;

/**
 * Unit tests for DynamoDBPersistenceStore using mocked DynamoDbClient.
 */
class DynamoDBPersistenceStoreTest {
    private static final String TABLE_NAME = "idempotency_table";
    private DynamoDbClient mockClient;
    private DynamoDBPersistenceStore persistenceStore;

    @BeforeEach
    void setup() {
        mockClient = mock(DynamoDbClient.class);
        persistenceStore = DynamoDBPersistenceStore.builder()
                .withTableName(TABLE_NAME)
                .withDynamoDbClient(mockClient)
                .build();
    }

    @Test
    void putRecord_shouldSendCorrectPutItemRequest() throws IdempotencyItemAlreadyExistsException {
        // GIVEN
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, null, null);

        // WHEN
        persistenceStore.putRecord(record, now);

        // THEN
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockClient).putItem(captor.capture());
        
        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.item()).containsEntry("id", AttributeValue.builder().s("key").build());
        assertThat(request.item().get("status").s()).isEqualTo("COMPLETED");
        assertThat(request.item().get("expiration").n()).isEqualTo(String.valueOf(expiry));
        assertThat(request.conditionExpression()).contains("attribute_not_exists(#id)");
        assertThat(request.conditionExpression()).contains("#expiry < :now");
    }

    @Test
    void putRecord_shouldIncludeInProgressExpiry_whenProvided() throws IdempotencyItemAlreadyExistsException {
        // GIVEN
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        long inProgressExpiry = now.plus(300, ChronoUnit.SECONDS).toEpochMilli();
        DataRecord record = new DataRecord("key", DataRecord.Status.INPROGRESS, expiry, null, null, OptionalLong.of(inProgressExpiry));

        // WHEN
        persistenceStore.putRecord(record, now);

        // THEN
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockClient).putItem(captor.capture());
        
        PutItemRequest request = captor.getValue();
        assertThat(request.item().get("in_progress_expiration").n()).isEqualTo(String.valueOf(inProgressExpiry));
    }

    @Test
    void putRecord_shouldThrowIdempotencyItemAlreadyExistsException_whenConditionFails() {
        // GIVEN
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.INPROGRESS, expiry, null, null);
        
        Map<String, AttributeValue> existingItem = new HashMap<>();
        existingItem.put("id", AttributeValue.builder().s("key").build());
        existingItem.put("status", AttributeValue.builder().s("COMPLETED").build());
        existingItem.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        existingItem.put("data", AttributeValue.builder().s("Existing Data").build());
        
        ConditionalCheckFailedException exception = ConditionalCheckFailedException.builder()
                .item(existingItem)
                .build();
        when(mockClient.putItem(any(PutItemRequest.class))).thenThrow(exception);

        // WHEN / THEN
        assertThatThrownBy(() -> persistenceStore.putRecord(record, now))
                .isInstanceOf(IdempotencyItemAlreadyExistsException.class)
                .matches(e -> ((IdempotencyItemAlreadyExistsException) e).getDataRecord().isPresent())
                .satisfies(e -> {
                    IdempotencyItemAlreadyExistsException ex = (IdempotencyItemAlreadyExistsException) e;
                    DataRecord existingRecord = ex.getDataRecord().get();
                    assertThat(existingRecord.getIdempotencyKey()).isEqualTo("key");
                    assertThat(existingRecord.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
                    assertThat(existingRecord.getResponseData()).isEqualTo("Existing Data");
                });
    }

    @Test
    void putRecord_shouldThrowWithoutDataRecord_whenConditionalCheckHasNoItem() {
        // GIVEN
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.INPROGRESS, expiry, null, null);
        
        ConditionalCheckFailedException exception = ConditionalCheckFailedException.builder().build();
        when(mockClient.putItem(any(PutItemRequest.class))).thenThrow(exception);

        // WHEN / THEN
        assertThatThrownBy(() -> persistenceStore.putRecord(record, now))
                .isInstanceOf(IdempotencyItemAlreadyExistsException.class)
                .matches(e -> !((IdempotencyItemAlreadyExistsException) e).getDataRecord().isPresent());
    }

    @Test
    void getRecord_shouldReturnDataRecord_whenItemExists() throws IdempotencyItemNotFoundException {
        // GIVEN
        long expiry = Instant.now().plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("key").build());
        item.put("status", AttributeValue.builder().s("COMPLETED").build());
        item.put("expiration", AttributeValue.builder().n(String.valueOf(expiry)).build());
        item.put("data", AttributeValue.builder().s("Response Data").build());
        
        GetItemResponse response = GetItemResponse.builder()
                .item(item)
                .build();
        when(mockClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        // WHEN
        DataRecord record = persistenceStore.getRecord("key");

        // THEN
        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());
        
        GetItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.consistentRead()).isTrue();
        assertThat(request.key()).containsEntry("id", AttributeValue.builder().s("key").build());
        
        assertThat(record.getIdempotencyKey()).isEqualTo("key");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
        assertThat(record.getExpiryTimestamp()).isEqualTo(expiry);
        assertThat(record.getResponseData()).isEqualTo("Response Data");
    }

    @Test
    void getRecord_shouldThrowException_whenItemDoesNotExist() {
        // GIVEN
        GetItemResponse response = GetItemResponse.builder().build();
        when(mockClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        // WHEN / THEN
        assertThatThrownBy(() -> persistenceStore.getRecord("key"))
                .isInstanceOf(IdempotencyItemNotFoundException.class);
    }

    @Test
    void updateRecord_shouldSendCorrectUpdateItemRequest() {
        // GIVEN
        long expiry = Instant.now().plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, "Response", null);

        // WHEN
        persistenceStore.updateRecord(record);

        // THEN
        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(mockClient).updateItem(captor.capture());
        
        UpdateItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.key()).containsEntry("id", AttributeValue.builder().s("key").build());
        assertThat(request.updateExpression()).contains("SET #response_data = :response_data");
        assertThat(request.updateExpression()).contains("#expiry = :expiry");
        assertThat(request.updateExpression()).contains("#status = :status");
        assertThat(request.expressionAttributeValues().get(":response_data").s()).isEqualTo("Response");
        assertThat(request.expressionAttributeValues().get(":status").s()).isEqualTo("COMPLETED");
    }

    @Test
    void updateRecord_shouldIncludeValidation_whenPayloadValidationEnabled() {
        // GIVEN
        persistenceStore.configure(IdempotencyConfig.builder().withPayloadValidationJMESPath("body").build(), null);
        long expiry = Instant.now().plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("key", DataRecord.Status.COMPLETED, expiry, "Response", "hash123");

        // WHEN
        persistenceStore.updateRecord(record);

        // THEN
        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(mockClient).updateItem(captor.capture());
        
        UpdateItemRequest request = captor.getValue();
        assertThat(request.updateExpression()).contains("#validation_key = :validation_key");
        assertThat(request.expressionAttributeValues().get(":validation_key").s()).isEqualTo("hash123");
    }

    @Test
    void deleteRecord_shouldSendCorrectDeleteItemRequest() {
        // WHEN
        persistenceStore.deleteRecord("key");

        // THEN
        ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(mockClient).deleteItem(captor.capture());
        
        DeleteItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.key()).containsEntry("id", AttributeValue.builder().s("key").build());
    }

    @Test
    void customAttributeNames_shouldUseCorrectAttributes() throws IdempotencyItemAlreadyExistsException {
        // GIVEN
        DynamoDBPersistenceStore customStore = DynamoDBPersistenceStore.builder()
                .withTableName("custom_table")
                .withDynamoDbClient(mockClient)
                .withKeyAttr("pk")
                .withSortKeyAttr("sk")
                .withStaticPkValue("IDEMPOTENCY")
                .withExpiryAttr("ttl")
                .withStatusAttr("state")
                .withDataAttr("result")
                .withValidationAttr("hash")
                .build();
        
        Instant now = Instant.now();
        long expiry = now.plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        DataRecord record = new DataRecord("mykey", DataRecord.Status.INPROGRESS, expiry, null, null);

        // WHEN
        customStore.putRecord(record, now);

        // THEN
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockClient).putItem(captor.capture());
        
        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("custom_table");
        assertThat(request.item()).containsEntry("pk", AttributeValue.builder().s("IDEMPOTENCY").build());
        assertThat(request.item()).containsEntry("sk", AttributeValue.builder().s("mykey").build());
        assertThat(request.item().get("state").s()).isEqualTo("INPROGRESS");
        assertThat(request.item().get("ttl").n()).isEqualTo(String.valueOf(expiry));
    }

    @Test
    void customAttributeNames_shouldUseCorrectKey_forGet() throws IdempotencyItemNotFoundException {
        // GIVEN
        DynamoDBPersistenceStore customStore = DynamoDBPersistenceStore.builder()
                .withTableName("custom_table")
                .withDynamoDbClient(mockClient)
                .withKeyAttr("pk")
                .withSortKeyAttr("sk")
                .withStaticPkValue("IDEMPOTENCY")
                .withExpiryAttr("ttl")
                .withStatusAttr("state")
                .build();
        
        long expiry = Instant.now().plus(3600, ChronoUnit.SECONDS).getEpochSecond();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s("IDEMPOTENCY").build());
        item.put("sk", AttributeValue.builder().s("mykey").build());
        item.put("state", AttributeValue.builder().s("COMPLETED").build());
        item.put("ttl", AttributeValue.builder().n(String.valueOf(expiry)).build());
        
        GetItemResponse response = GetItemResponse.builder().item(item).build();
        when(mockClient.getItem(any(GetItemRequest.class))).thenReturn(response);

        // WHEN
        DataRecord record = customStore.getRecord("mykey");

        // THEN
        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(mockClient).getItem(captor.capture());
        
        GetItemRequest request = captor.getValue();
        assertThat(request.key()).containsEntry("pk", AttributeValue.builder().s("IDEMPOTENCY").build());
        assertThat(request.key()).containsEntry("sk", AttributeValue.builder().s("mykey").build());
        
        assertThat(record.getIdempotencyKey()).isEqualTo("mykey");
        assertThat(record.getStatus()).isEqualTo(DataRecord.Status.COMPLETED);
    }

    @Test
    @SetEnvironmentVariable(key = Constants.IDEMPOTENCY_DISABLED_ENV, value = "true")
    void idempotencyDisabled_noClientShouldBeCreated() {
        // GIVEN / WHEN
        DynamoDBPersistenceStore store = DynamoDBPersistenceStore.builder()
                .withTableName(TABLE_NAME)
                .build();

        // THEN
        assertThatThrownBy(() -> store.getRecord("fake"))
                .isInstanceOf(NullPointerException.class);
    }
}
