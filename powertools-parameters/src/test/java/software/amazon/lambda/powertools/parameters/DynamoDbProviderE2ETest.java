package software.amazon.lambda.powertools.parameters;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class provides simple end-to-end style testing of the DynamoDBProvider class.
 * It is ignored, for now, as it requires AWS access and that's not yet run as part
 * of our unit test suite in the cloud.
 *
 * The test is kept here for 1/ local development and 2/ in preparation for future
 * E2E tests running in the cloud CI. Once the E2E test structure is merged we
 * will move this across.
 */
@Disabled
public class DynamoDbProviderE2ETest {

    final String ParamsTestTable = "ddb-params-test";
    final String MultiparamsTestTable = "ddb-multiparams-test";
    private final DynamoDbClient ddbClient;

    public DynamoDbProviderE2ETest() {
        // Create a DDB client to inject test data into our test tables
        ddbClient = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();


    }

    @Test
    public void TestGetValue() {

        // Arrange
        HashMap<String, AttributeValue> testItem = new HashMap<String, AttributeValue>();
        testItem.put("id", AttributeValue.fromS("test_param"));
        testItem.put("value", AttributeValue.fromS("the_value_is_hello!"));
        ddbClient.putItem(PutItemRequest.builder()
                        .tableName(ParamsTestTable)
                        .item(testItem)
                .build());

        // Act
        DynamoDbProvider provider = makeProvider(ParamsTestTable);
        String value = provider.getValue("test_param");

        // Assert
        assertThat(value).isEqualTo("the_value_is_hello!");
    }

    @Test
    public void TestGetValues() {

        // Arrange
        HashMap<String, AttributeValue> testItem = new HashMap<String, AttributeValue>();
        testItem.put("id", AttributeValue.fromS("test_param"));
        testItem.put("sk", AttributeValue.fromS("test_param_part_1"));
        testItem.put("value", AttributeValue.fromS("the_value_is_hello!"));
        ddbClient.putItem(PutItemRequest.builder()
                .tableName(MultiparamsTestTable)
                .item(testItem)
                .build());

        HashMap<String, AttributeValue> testItem2 = new HashMap<String, AttributeValue>();
        testItem2.put("id", AttributeValue.fromS("test_param"));
        testItem2.put("sk", AttributeValue.fromS("test_param_part_2"));
        testItem2.put("value", AttributeValue.fromS("the_value_is_still_hello!"));
        ddbClient.putItem(PutItemRequest.builder()
                .tableName(MultiparamsTestTable)
                .item(testItem2)
                .build());

        // Act
        DynamoDbProvider provider = makeProvider(MultiparamsTestTable);
        Map<String, String> values = provider.getMultipleValues("test_param");

        // Assert
        assertThat(values.size()).isEqualTo(2);
        assertThat(values.get("test_param_part_1")).isEqualTo("the_value_is_hello!");
        assertThat(values.get("test_param_part_2")).isEqualTo("the_value_is_still_hello!");
    }

    private DynamoDbProvider makeProvider(String tableName) {
        return new DynamoDbProvider(new CacheManager(), DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder()).build(),
                tableName);
    }

}
