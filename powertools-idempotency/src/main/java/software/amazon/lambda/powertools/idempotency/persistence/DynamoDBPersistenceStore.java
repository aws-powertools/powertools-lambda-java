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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.lambda.powertools.idempotency.Constants;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemAlreadyExistsException;
import software.amazon.lambda.powertools.idempotency.exceptions.IdempotencyItemNotFoundException;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.lambda.powertools.idempotency.Constants.AWS_REGION_ENV;
import static software.amazon.lambda.powertools.idempotency.persistence.DataRecord.Status.INPROGRESS;

/**
 * DynamoDB version of the {@link PersistenceStore}. Will store idempotency data in DynamoDB.<br>
 * Use the {@link Builder} to create a new instance.
 */
public class DynamoDBPersistenceStore extends BasePersistenceStore implements PersistenceStore {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBPersistenceStore.class);

    private final String tableName;
    private final String keyAttr;
    private final String staticPkValue;
    private final String sortKeyAttr;
    private final String expiryAttr;

    private final String inProgressExpiryAttr;
    private final String statusAttr;
    private final String dataAttr;
    private final String validationAttr;
    private final DynamoDbClient dynamoDbClient;

    /**
     * Private: use the {@link Builder} to instantiate a new {@link DynamoDBPersistenceStore}
     */
    private DynamoDBPersistenceStore(String tableName,
                                     String keyAttr,
                                     String staticPkValue,
                                     String sortKeyAttr,
                                     String expiryAttr,
                                     String inProgressExpiryAttr,
                                     String statusAttr,
                                     String dataAttr,
                                     String validationAttr,
                                     DynamoDbClient client) {
        this.tableName = tableName;
        this.keyAttr = keyAttr;
        this.staticPkValue = staticPkValue;
        this.sortKeyAttr = sortKeyAttr;
        this.expiryAttr = expiryAttr;
        this.inProgressExpiryAttr = inProgressExpiryAttr;
        this.statusAttr = statusAttr;
        this.dataAttr = dataAttr;
        this.validationAttr = validationAttr;

        if (client != null) {
            this.dynamoDbClient = client;
        } else {
            String idempotencyDisabledEnv = System.getenv().get(Constants.IDEMPOTENCY_DISABLED_ENV);
            if (idempotencyDisabledEnv == null || idempotencyDisabledEnv.equalsIgnoreCase("false")) {
                DynamoDbClientBuilder ddbBuilder = DynamoDbClient.builder()
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .region(Region.of(System.getenv(AWS_REGION_ENV)));
                this.dynamoDbClient = ddbBuilder.build();
            } else {
                // we do not want to create a DynamoDbClient if idempotency is disabled
                // null is ok as idempotency won't be called
                this.dynamoDbClient = null;
            }
        }
    }

    @Override
    public DataRecord getRecord(String idempotencyKey) throws IdempotencyItemNotFoundException {
        GetItemResponse response = dynamoDbClient.getItem(
                GetItemRequest.builder()
                        .tableName(tableName)
                        .key(getKey(idempotencyKey))
                        .consistentRead(true)
                        .build()
        );

        if (!response.hasItem()) {
            throw new IdempotencyItemNotFoundException(idempotencyKey);
        }

        return itemToRecord(response.item());
    }

    @Override
    public void putRecord(DataRecord record, Instant now) throws IdempotencyItemAlreadyExistsException {
        Map<String, AttributeValue> item = new HashMap<>(getKey(record.getIdempotencyKey()));
        item.put(this.expiryAttr, AttributeValue.builder().n(String.valueOf(record.getExpiryTimestamp())).build());
        item.put(this.statusAttr, AttributeValue.builder().s(record.getStatus().toString()).build());

        if (record.getInProgressExpiryTimestamp().isPresent()) {
            item.put(this.inProgressExpiryAttr, AttributeValue.builder().n(String.valueOf(record.getInProgressExpiryTimestamp().getAsLong())).build());
        }

        if (this.payloadValidationEnabled) {
            item.put(this.validationAttr, AttributeValue.builder().s(record.getPayloadHash()).build());
        }

        try {
            LOG.debug("Putting record for idempotency key: {}", record.getIdempotencyKey());

            Map<String, String> expressionAttributeNames = Stream.of(
                            new AbstractMap.SimpleEntry<>("#id", this.keyAttr),
                            new AbstractMap.SimpleEntry<>("#expiry", this.expiryAttr),
                            new AbstractMap.SimpleEntry<>("#in_progress_expiry", this.inProgressExpiryAttr),
                            new AbstractMap.SimpleEntry<>("#status", this.statusAttr))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<String, AttributeValue> expressionAttributeValues = Stream.of(
                    new AbstractMap.SimpleEntry<>(":now", AttributeValue.builder().n(String.valueOf(now.getEpochSecond())).build()),
                    new AbstractMap.SimpleEntry<>(":inprogress", AttributeValue.builder().s(INPROGRESS.toString()).build())
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            dynamoDbClient.putItem(
                    PutItemRequest.builder()
                            .tableName(tableName)
                            .item(item)
                            .conditionExpression("attribute_not_exists(#id) OR #expiry < :now OR (attribute_exists(#in_progress_expiry) AND #in_progress_expiry < :now AND #status = :inprogress)")
                            .expressionAttributeNames(expressionAttributeNames)
                            .expressionAttributeValues(expressionAttributeValues)
                            .build()
            );
        } catch (ConditionalCheckFailedException e) {
            LOG.debug("Failed to put record for already existing idempotency key: {}", record.getIdempotencyKey());
            throw new IdempotencyItemAlreadyExistsException("Failed to put record for already existing idempotency key: " + record.getIdempotencyKey(), e);
        }
    }

    @Override
    public void updateRecord(DataRecord record) {
        LOG.debug("Updating record for idempotency key: {}", record.getIdempotencyKey());
        String updateExpression = "SET #response_data = :response_data, #expiry = :expiry, #status = :status";

        Map<String, String> expressionAttributeNames = Stream.of(
                        new AbstractMap.SimpleEntry<>("#response_data", this.dataAttr),
                        new AbstractMap.SimpleEntry<>("#expiry", this.expiryAttr),
                        new AbstractMap.SimpleEntry<>("#status", this.statusAttr))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, AttributeValue> expressionAttributeValues = Stream.of(
                        new AbstractMap.SimpleEntry<>(":response_data", AttributeValue.builder().s(record.getResponseData()).build()),
                        new AbstractMap.SimpleEntry<>(":expiry", AttributeValue.builder().n(String.valueOf(record.getExpiryTimestamp())).build()),
                        new AbstractMap.SimpleEntry<>(":status", AttributeValue.builder().s(record.getStatus().toString()).build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (payloadValidationEnabled) {
            updateExpression += ", #validation_key = :validation_key";
            expressionAttributeNames.put("#validation_key", this.validationAttr);
            expressionAttributeValues.put(":validation_key", AttributeValue.builder().s(record.getPayloadHash()).build());
        }

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(getKey(record.getIdempotencyKey()))
                .updateExpression(updateExpression)
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build()
        );
    }

    @Override
    public void deleteRecord(String idempotencyKey) {
        LOG.debug("Deleting record for idempotency key: {}", idempotencyKey);
        dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(getKey(idempotencyKey))
                .build()
        );
    }

    /**
     * Get the key to use for requests (depending on if we have a sort key or not)
     *
     * @param idempotencyKey
     * @return
     */
    private Map<String, AttributeValue> getKey(String idempotencyKey) {
        Map<String, AttributeValue> key = new HashMap<>();
        if (this.sortKeyAttr != null) {
            key.put(this.keyAttr, AttributeValue.builder().s(this.staticPkValue).build());
            key.put(this.sortKeyAttr, AttributeValue.builder().s(idempotencyKey).build());
        } else {
            key.put(this.keyAttr, AttributeValue.builder().s(idempotencyKey).build());
        }
        return key;
    }

    /**
     * Translate raw item records from DynamoDB to DataRecord
     *
     * @param item Item from dynamodb response
     * @return DataRecord instance
     */
    private DataRecord itemToRecord(Map<String, AttributeValue> item) {
        // data and validation payload may be null
        AttributeValue data = item.get(this.dataAttr);
        AttributeValue validation = item.get(this.validationAttr);
        return new DataRecord(item.get(sortKeyAttr != null ? sortKeyAttr: keyAttr).s(),
                DataRecord.Status.valueOf(item.get(this.statusAttr).s()),
                Long.parseLong(item.get(this.expiryAttr).n()),
                data != null ? data.s() : null,
                validation != null ? validation.s() : null,
                item.get(this.inProgressExpiryAttr) != null ? OptionalLong.of(Long.parseLong(item.get(this.inProgressExpiryAttr).n())) : OptionalLong.empty());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Use this builder to get an instance of {@link DynamoDBPersistenceStore}.<br/>
     * With this builder you can configure the characteristics of the DynamoDB Table
     * (name, key, sort key, and other field names).<br/>
     * You can also set a custom {@link DynamoDbClient} for further tuning.
     */
    public static class Builder {
        private static final String funcEnv = System.getenv(Constants.LAMBDA_FUNCTION_NAME_ENV);

        private String tableName;
        private String keyAttr = "id";
        private String staticPkValue = String.format("idempotency#%s", funcEnv != null ? funcEnv : "");
        private String sortKeyAttr;
        private String expiryAttr = "expiration";

        private String inProgressExpiryAttr = "in_progress_expiration";
        private String statusAttr = "status";
        private String dataAttr = "data";
        private String validationAttr = "validation";
        private DynamoDbClient dynamoDbClient;

        /**
         * Initialize and return a new instance of {@link DynamoDBPersistenceStore}.<br/>
         * Example:<br>
         * <pre>
         *     DynamoDBPersistenceStore.builder().withTableName("idempotency_store").build();
         * </pre>
         *
         * @return an instance of the {@link DynamoDBPersistenceStore}
         */
        public DynamoDBPersistenceStore build() {
            if (StringUtils.isEmpty(tableName)) {
                throw new IllegalArgumentException("Table name is not specified");
            }
            return new DynamoDBPersistenceStore(tableName, keyAttr, staticPkValue, sortKeyAttr, expiryAttr, inProgressExpiryAttr, statusAttr, dataAttr, validationAttr, dynamoDbClient);
        }

        /**
         * Name of the table to use for storing execution records (mandatory)
         *
         * @param tableName Name of the DynamoDB table
         * @return the builder instance (to chain operations)
         */
        public Builder withTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * DynamoDB attribute name for partition key (optional), by default "id"
         *
         * @param keyAttr name of the key attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withKeyAttr(String keyAttr) {
            this.keyAttr = keyAttr;
            return this;
        }

        /**
         * DynamoDB attribute value for partition key (optional), by default "idempotency#[function-name]".
         * This will be used if the {@link #sortKeyAttr} is set.
         *
         * @param staticPkValue name of the partition key attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withStaticPkValue(String staticPkValue) {
            this.staticPkValue = staticPkValue;
            return this;
        }

        /**
         * DynamoDB attribute name for the sort key (optional)
         *
         * @param sortKeyAttr name of the sort key attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withSortKeyAttr(String sortKeyAttr) {
            this.sortKeyAttr = sortKeyAttr;
            return this;
        }

        /**
         * DynamoDB attribute name for expiry timestamp (optional), by default "expiration"
         *
         * @param expiryAttr name of the expiry attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withExpiryAttr(String expiryAttr) {
            this.expiryAttr = expiryAttr;
            return this;
        }

        /**
         * DynamoDB attribute name for in progress expiry timestamp (optional), by default "in_progress_expiration"
         *
         * @param inProgressExpiryAttr name of the attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withInProgressExpiryAttr(String inProgressExpiryAttr) {
            this.inProgressExpiryAttr = inProgressExpiryAttr;
            return this;
        }

        /**
         * DynamoDB attribute name for status (optional), by default "status"
         *
         * @param statusAttr name of the status attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withStatusAttr(String statusAttr) {
            this.statusAttr = statusAttr;
            return this;
        }

        /**
         * DynamoDB attribute name for response data (optional), by default "data"
         *
         * @param dataAttr name of the data attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withDataAttr(String dataAttr) {
            this.dataAttr = dataAttr;
            return this;
        }

        /**
         * DynamoDB attribute name for validation (optional), by default "validation"
         *
         * @param validationAttr name of the validation attribute in the table
         * @return the builder instance (to chain operations)
         */
        public Builder withValidationAttr(String validationAttr) {
            this.validationAttr = validationAttr;
            return this;
        }

        /**
         * Custom {@link DynamoDbClient} used to query DynamoDB (optional).<br/>
         * The default one uses {@link UrlConnectionHttpClient} as a http client and
         * add com.amazonaws.xray.interceptors.TracingInterceptor (X-Ray) if available in the classpath.
         *
         * @param dynamoDbClient the {@link DynamoDbClient} instance to use
         * @return the builder instance (to chain operations)
         */
        public Builder withDynamoDbClient(DynamoDbClient dynamoDbClient) {
            this.dynamoDbClient = dynamoDbClient;
            return this;
        }
    }
}
