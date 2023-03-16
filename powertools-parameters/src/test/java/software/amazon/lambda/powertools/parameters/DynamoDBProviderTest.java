package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

public class DynamoDBProviderTest {

    @Mock
    DynamoDbClient client;

    @Captor
    ArgumentCaptor<GetItemRequest> getItemValueCaptor;

    @Captor
    ArgumentCaptor<QueryRequest> queryRequestCaptor;


    private DynamoDBProvider provider;
    private final String tableName = "ddb-test-table";

    @BeforeEach
    public void init() {
        openMocks(this);
        CacheManager cacheManager = new CacheManager();
        provider = new DynamoDBProvider(cacheManager, client, tableName);
    }


    @Test
    public void getValue() {

        // Arrange
        String key = "Key1";
        String expectedValue = "Value1";
        HashMap<String, AttributeValue> responseData = new HashMap<String, AttributeValue>();
        responseData.put("id", AttributeValue.fromS(key));
        responseData.put("val", AttributeValue.fromS(expectedValue));
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
    public void getValueWithoutResultsReturnsNull() {
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
    public void getValueWithMalformedRowThrows() {
        // Arrange
        String key = "Key1";
        HashMap<String, AttributeValue> responseData = new HashMap<String, AttributeValue>();
        responseData.put("id", AttributeValue.fromS(key));
        responseData.put("not-val", AttributeValue.fromS("something"));
        Mockito.when(client.getItem(getItemValueCaptor.capture())).thenReturn(GetItemResponse.builder()
                .item(responseData)
                .build());
        // Act
        Assertions.assertThrows(NullPointerException.class, () -> {
            String value = provider.getValue(key);
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
        item1.put("val", AttributeValue.fromS(val1));
        HashMap<String, AttributeValue> item2 = new HashMap<String, AttributeValue>();
        item2.put("id", AttributeValue.fromS(key));
        item2.put("sk", AttributeValue.fromS(subkey2));
        item2.put("val", AttributeValue.fromS(val2));
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
}
