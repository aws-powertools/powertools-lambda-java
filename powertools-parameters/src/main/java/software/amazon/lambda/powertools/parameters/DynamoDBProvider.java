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
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
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

        // TODO
        return resp.item().values().stream().findFirst().get().s();
    }

    @Override
    protected Map<String, String> getMultipleValues(String path) {

        QueryResponse resp = client.query(QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("id = :v_id")
                .expressionAttributeValues(Collections.singletonMap("v_id", AttributeValue.fromS(path)))
                .attributesToGet("sk", "value")
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
