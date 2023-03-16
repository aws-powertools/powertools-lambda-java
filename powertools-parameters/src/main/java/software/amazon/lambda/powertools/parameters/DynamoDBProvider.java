package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamoDBProvider extends BaseProvider {

    private final DynamoDbClient client;
    private final String tableName;

    public DynamoDBProvider(CacheManager cacheManager, String tableName) {
        this(cacheManager, DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                //.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                //.region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                .build(),
                tableName
        );

    }

    DynamoDBProvider(CacheManager cacheManager, DynamoDbClient client, String tableName) {
        super(cacheManager);
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    protected String getValue(String key) {
        GetItemResponse resp = client.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Collections.singletonMap("id", AttributeValue.fromS(key)))
                .attributesToGet("val")
                .build());

        // If we have an item at the key, we should be able to get a 'val' out of it. If not it's
        // exceptional.
        // If we don't have an item at the key, we should return null.
        if (resp.hasItem() && !resp.item().values().isEmpty()) {
            return resp.item().get("val").s();
        }

        return null;
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {

        QueryResponse resp = client.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("id = :v_id")
                .expressionAttributeValues(Collections.singletonMap(":v_id", AttributeValue.fromS(path)))
                .build());

        return resp
                    .items()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    (i) -> i.get("sk").s(),
                                    (i) -> i.get("val").s()));


    }
}
