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

package software.amazon.lambda.powertools.parameters.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.dynamodb.exception.DynamoDbProviderSchemaException;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

public class DynamoDbProviderTest {

    private final String tableName = "ddb-test-table";
    @Mock
    DynamoDbClient client;
    @Mock
    TransformationManager transformationManager;
    @Captor
    ArgumentCaptor<GetItemRequest> getItemValueCaptor;
    @Captor
    ArgumentCaptor<QueryRequest> queryRequestCaptor;
    private DynamoDbProvider provider;

    @BeforeEach
    public void init() {
        openMocks(this);
        CacheManager cacheManager = new CacheManager();
        provider = new DynamoDbProvider(cacheManager, transformationManager, client, tableName);
    }


    @Test
    public void getValue() {

        // Arrange
        String key = "Key1";
        String expectedValue = "Value1";
        HashMap<String, AttributeValue> responseData = new HashMap<String, AttributeValue>();
        responseData.put("id", AttributeValue.fromS(key));
        responseData.put("value", AttributeValue.fromS(expectedValue));
        GetItemResponse response = GetItemResponse.builder()
                .item(responseData)
                .build();
        Mockito.when(client.getItem(getItemValueCaptor.capture())).thenReturn(response);

        // Act
        String value = provider.getValue(key);

        // Assert
        assertThat(value).isEqualTo(expectedValue);
        assertThat(getItemValueCaptor.getValue().tableName()).isEqualTo(tableName);
        assertThat(getItemValueCaptor.getValue().key().get("id").s()).isEqualTo(key);
    }


    @Test
    public void getValueWithNullResultsReturnsNull() {
        // Arrange
        Mockito.when(client.getItem(getItemValueCaptor.capture())).thenReturn(GetItemResponse.builder()
                .item(null)
                .build());

        // Act
        String value = provider.getValue("key");

        // Assert
        assertThat(value).isEqualTo(null);
    }

    @Test
    public void getValueWithoutResultsReturnsNull() {
        // Arrange
        Mockito.when(client.getItem(getItemValueCaptor.capture())).thenReturn(GetItemResponse.builder()
                .item(new HashMap<>())
                .build());

        // Act
        String value = provider.getValue("key");

        // Assert
        assertThat(value).isEqualTo(null);
    }

    @Test
    public void getValueWithMalformedRowThrows() {
        // Arrange
        String key = "Key1";
        HashMap<String, AttributeValue> responseData = new HashMap<String, AttributeValue>();
        responseData.put("id", AttributeValue.fromS(key));
        responseData.put("not-value", AttributeValue.fromS("something"));
        Mockito.when(client.getItem(getItemValueCaptor.capture())).thenReturn(GetItemResponse.builder()
                .item(responseData)
                .build());
        // Act
        Assertions.assertThrows(DynamoDbProviderSchemaException.class, () ->
        {
            provider.getValue(key);
        });
    }


    @Test
    public void getValues() {

        // Arrange
        String key = "Key1";
        String subkey1 = "Subkey1";
        String val1 = "Val1";
        String subkey2 = "Subkey2";
        String val2 = "Val2";
        HashMap<String, AttributeValue> item1 = new HashMap<String, AttributeValue>();
        item1.put("id", AttributeValue.fromS(key));
        item1.put("sk", AttributeValue.fromS(subkey1));
        item1.put("value", AttributeValue.fromS(val1));
        HashMap<String, AttributeValue> item2 = new HashMap<String, AttributeValue>();
        item2.put("id", AttributeValue.fromS(key));
        item2.put("sk", AttributeValue.fromS(subkey2));
        item2.put("value", AttributeValue.fromS(val2));
        QueryResponse response = QueryResponse.builder()
                .items(item1, item2)
                .build();
        Mockito.when(client.query(queryRequestCaptor.capture())).thenReturn(response);

        // Act
        Map<String, String> values = provider.getMultipleValues(key);

        // Assert
        assertThat(values.size()).isEqualTo(2);
        assertThat(values.get(subkey1)).isEqualTo(val1);
        assertThat(values.get(subkey2)).isEqualTo(val2);
        assertThat(queryRequestCaptor.getValue().tableName()).isEqualTo(tableName);
        assertThat(queryRequestCaptor.getValue().keyConditionExpression()).isEqualTo("id = :v_id");
        assertThat(queryRequestCaptor.getValue().expressionAttributeValues().get(":v_id").s()).isEqualTo(key);
    }

    @Test
    public void getValuesWithoutResultsReturnsNull() {
        // Arrange
        Mockito.when(client.query(queryRequestCaptor.capture())).thenReturn(
                QueryResponse.builder().items().build());

        // Act
        Map<String, String> values = provider.getMultipleValues(UUID.randomUUID().toString());

        // Assert
        assertThat(values.size()).isEqualTo(0);
    }

    @Test
    public void getMultipleValuesMissingSortKey_throwsException() {
        // Arrange
        String key = "Key1";
        HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", AttributeValue.fromS(key));
        item.put("value", AttributeValue.fromS("somevalue"));
        QueryResponse response = QueryResponse.builder()
                .items(item)
                .build();
        Mockito.when(client.query(queryRequestCaptor.capture())).thenReturn(response);

        // Assert
        Assertions.assertThrows(DynamoDbProviderSchemaException.class, () ->
        {
            // Act
            provider.getMultipleValues(key);
        });
    }

    @Test
    public void getValuesWithMalformedRowThrows() {
        // Arrange
        String key = "Key1";
        HashMap<String, AttributeValue> item1 = new HashMap<String, AttributeValue>();
        item1.put("id", AttributeValue.fromS(key));
        item1.put("sk", AttributeValue.fromS("some-subkey"));
        item1.put("not-value", AttributeValue.fromS("somevalue"));
        QueryResponse response = QueryResponse.builder()
                .items(item1)
                .build();
        Mockito.when(client.query(queryRequestCaptor.capture())).thenReturn(response);

        // Assert
        Assertions.assertThrows(DynamoDbProviderSchemaException.class, () ->
        {
            // Act
            provider.getMultipleValues(key);
        });
    }

    @Test
    public void testDynamoDBBuilderMissingTable_throwsException() {

        // Act & Assert
        assertThatIllegalStateException().isThrownBy(() -> DynamoDbProvider.builder()
                .withCacheManager(new CacheManager())
                .build());
    }

}
