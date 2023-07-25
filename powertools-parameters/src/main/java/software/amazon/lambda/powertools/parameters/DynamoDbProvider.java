package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.exception.DynamoDbProviderSchemaException;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements a {@link ParamProvider} on top of DynamoDB. The schema of the table
 * is described in the Powertools for AWS Lambda (Java) documentation.
 *
 * @see <a href="https://docs.powertools.aws.dev/lambda-java/utilities/parameters">Parameters provider documentation</a>
 *
 */
public class DynamoDbProvider extends BaseProvider {

    private static final String VALUE = "value";
    private final DynamoDbClient client;
    private final String tableName;

    DynamoDbProvider(CacheManager cacheManager, DynamoDbClient client, String tableName) {
        super(cacheManager);
        this.client = client;
        this.tableName = tableName;
    }

    DynamoDbProvider(CacheManager cacheManager, String tableName) {
        this(cacheManager, Builder.createClient(), tableName);
    }

    /**
     * Return a single value from the DynamoDB parameter provider.
     *
     * @param key key of the parameter
     * @return The value, if it exists, null if it doesn't. Throws if the row exists but doesn't match the schema.
     */
    @Override
    protected String getValue(String key) {
        GetItemResponse resp = client.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Collections.singletonMap("id", AttributeValue.fromS(key)))
                .attributesToGet(VALUE)
                .build());

        // If we have an item at the key, we should be able to get a 'val' out of it. If not it's
        // exceptional.
        // If we don't have an item at the key, we should return null.
        if (resp.hasItem() && !resp.item().values().isEmpty()) {
            if (!resp.item().containsKey(VALUE)) {
                throw new DynamoDbProviderSchemaException("Missing 'value': " + resp.item().toString());
            }
            return resp.item().get(VALUE).s();
        }

        return null;
    }

    /**
     * Returns multiple values from the DynamoDB parameter provider.
     *
     * @param path Parameter store path
     * @return All values matching the given path, and an empty map if none do. Throws if any records exist that don't match the schema.
     */
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
                    .peek((i) -> {
                        if (!i.containsKey("sk")) {
                            throw new DynamoDbProviderSchemaException("Missing 'sk': " + i.toString());
                        }
                        if (!i.containsKey(VALUE)) {
                            throw new DynamoDbProviderSchemaException("Missing 'value': " + i.toString());
                        }
                    })
                    .collect(
                            Collectors.toMap(
                                    (i) -> i.get("sk").s(),
                                    (i) -> i.get(VALUE).s()));


    }

    /**
     * Create a builder that can be used to configure and create a {@link DynamoDbProvider}.
     *
     * @return a new instance of {@link DynamoDbProvider.Builder}
     */
    public static DynamoDbProvider.Builder builder() {
        return new DynamoDbProvider.Builder();
    }

    static class Builder {
        private DynamoDbClient client;
        private String table;
        private CacheManager cacheManager;
        private TransformationManager transformationManager;

        /**
         * Create a {@link DynamoDbProvider} instance.
         *
         * @return a {@link DynamoDbProvider}
         */
        public DynamoDbProvider build() {
            if (cacheManager == null) {
                throw new IllegalStateException("No CacheManager provided; please provide one");
            }
            if (table == null) {
                throw new IllegalStateException("No DynamoDB table name provided; please provide one");
            }
            DynamoDbProvider provider;
            if (client == null) {
                client = createClient();
            }
            provider = new DynamoDbProvider(cacheManager, client, table);

            if (transformationManager != null) {
                provider.setTransformationManager(transformationManager);
            }
            return provider;
        }

        /**
         * Set custom {@link DynamoDbClient} to pass to the {@link DynamoDbClient}. <br/>
         * Use it if you want to customize the region or any other part of the client.
         *
         * @param client Custom client
         * @return the builder to chain calls (eg. <pre>builder.withClient().build()</pre>)
         */
        public DynamoDbProvider.Builder withClient(DynamoDbClient client) {
            this.client = client;
            return this;
        }

        /**
         * <b>Mandatory</b>. Provide a CacheManager to the {@link DynamoDbProvider}
         *
         * @param cacheManager the manager that will handle the cache of parameters
         * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
         */
        public DynamoDbProvider.Builder withCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        /**
         * <b>Mandatory</b>. Provide a DynamoDB table to the {@link DynamoDbProvider}
         *
         * @param table the table that parameters will be retrieved from.
         * @return the builder to chain calls (eg. <pre>builder.withTable().build()</pre>)
         */
        public DynamoDbProvider.Builder withTable(String table) {
            this.table = table;
            return this;
        }

        /**
         * Provide a transformationManager to the {@link DynamoDbProvider}
         *
         * @param transformationManager the manager that will handle transformation of parameters
         * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
         */
        public DynamoDbProvider.Builder withTransformationManager(TransformationManager transformationManager) {
            this.transformationManager = transformationManager;
            return this;
        }

        private static DynamoDbClient createClient() {
            return DynamoDbClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                    .build();
        }
    }
}
