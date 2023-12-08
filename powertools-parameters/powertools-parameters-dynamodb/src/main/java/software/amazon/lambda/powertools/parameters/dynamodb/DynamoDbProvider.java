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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.lambda.powertools.parameters.BaseProvider;
import software.amazon.lambda.powertools.parameters.ParamProvider;
import software.amazon.lambda.powertools.parameters.cache.CacheManager;
import software.amazon.lambda.powertools.parameters.dynamodb.exception.DynamoDbProviderSchemaException;
import software.amazon.lambda.powertools.parameters.transform.TransformationManager;

/**
 * Implements a {@link ParamProvider} on top of Amazon DynamoDB. The schema of the table
 * is described in the Powertools for AWS Lambda (Java) documentation.
 *
 * @see <a href="https://docs.powertools.aws.dev/lambda-java/utilities/parameters">Parameters provider documentation</a>
 */
public class DynamoDbProvider extends BaseProvider {

    private final DynamoDbClient client;
    private final String tableName;

    DynamoDbProvider(CacheManager cacheManager, TransformationManager transformationManager, DynamoDbClient client,
                     String tableName) {
        super(cacheManager, transformationManager);
        this.client = client;
        this.tableName = tableName;
    }

    /**
     * Create a builder that can be used to configure and create a {@link DynamoDbProvider}.
     *
     * @return a new instance of {@link DynamoDbProviderBuilder}
     */
    public static DynamoDbProviderBuilder builder() {
        return new DynamoDbProviderBuilder();
    }

    /**
     * Create a DynamoDbProvider with all default settings.
     */
    public static DynamoDbProvider create() {
        return new DynamoDbProviderBuilder().build();
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
                .attributesToGet("value")
                .build());

        // If we have an item at the key, we should be able to get a 'val' out of it. If not it's
        // exceptional.
        // If we don't have an item at the key, we should return null.
        if (resp.hasItem() && !resp.item().values().isEmpty()) {
            if (!resp.item().containsKey("value")) {
                throw new DynamoDbProviderSchemaException("Missing 'value': " + resp.item());
            }
            return resp.item().get("value").s();
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
                .peek((i) ->
                {
                    if (!i.containsKey("sk")) {
                        throw new DynamoDbProviderSchemaException("Missing 'sk': " + i);
                    }
                    if (!i.containsKey("value")) {
                        throw new DynamoDbProviderSchemaException("Missing 'value': " + i);
                    }
                })
                .collect(
                        Collectors.toMap(
                                (i) -> i.get("sk").s(),
                                (i) -> i.get("value").s()));


    }

}
