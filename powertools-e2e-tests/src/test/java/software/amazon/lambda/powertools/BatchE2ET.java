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

package software.amazon.lambda.powertools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.lambda.powertools.testutils.DataNotReadyException;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.RetryUtils;
import software.amazon.lambda.powertools.utilities.JsonConfig;

class BatchE2ET {
    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
    private static Infrastructure infrastructure;
    private static String queueUrl;
    private static String kinesisStreamName;

    private static ObjectMapper objectMapper;
    private static String outputTable;
    private static DynamoDbClient ddbClient;
    private static SqsClient sqsClient;
    private static KinesisClient kinesisClient;
    private static String ddbStreamsTestTable;
    private final List<Product> testProducts;

    public BatchE2ET() {
        testProducts = Arrays.asList(
                new Product(1, "product1", 1.23),
                new Product(2, "product2", 4.56),
                new Product(3, "product3", 6.78));
    }

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    static void setup() {
        String random = UUID.randomUUID().toString().substring(0, 6);
        String queueName = "batchqueue" + random;
        kinesisStreamName = "batchstream" + random;
        ddbStreamsTestTable = "ddbstreams" + random;

        objectMapper = JsonConfig.get().getObjectMapper();

        infrastructure = Infrastructure.builder()
                .testName(BatchE2ET.class.getSimpleName())
                .pathToFunction("batch")
                .queue(queueName)
                .ddbStreamsTableName(ddbStreamsTestTable)
                .kinesisStream(kinesisStreamName)
                .build();

        Map<String, String> outputs = infrastructure.deploy();
        queueUrl = outputs.get("QueueURL");
        kinesisStreamName = outputs.get("KinesisStreamName");
        outputTable = outputs.get("TableNameForAsyncTests");
        ddbStreamsTestTable = outputs.get("DdbStreamsTestTable");

        ddbClient = DynamoDbClient.builder()
                .region(region)
                .httpClient(httpClient)
                .build();

        // GIVEN
        sqsClient = SqsClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
        kinesisClient = KinesisClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @AfterEach
    void cleanUpTest() {
        // Delete everything in the output table
        ScanResponse items = ddbClient.scan(ScanRequest.builder()
                .tableName(outputTable)
                .build());

        for (Map<String, AttributeValue> item : items.items()) {
            Map<String, AttributeValue> key = new HashMap<String, AttributeValue>() {
                {
                    put("functionName", AttributeValue.builder()
                            .s(item.get("functionName").s())
                            .build());
                    put("id", AttributeValue.builder()
                            .s(item.get("id").s())
                            .build());
                }
            };

            ddbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(outputTable)
                    .key(key)
                    .build());
        }
    }

    @Test
    void sqsBatchProcessingSucceeds() {
        List<SendMessageBatchRequestEntry> entries = testProducts.stream()
                .map(p -> {
                    try {
                        return SendMessageBatchRequestEntry.builder()
                                .id(p.getName())
                                .messageBody(objectMapper.writeValueAsString(p))
                                .build();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        // WHEN
        sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build());

        // THEN
        ScanRequest scanRequest = ScanRequest.builder().tableName(outputTable).build();
        RetryUtils.withRetry(() -> {
            ScanResponse items = ddbClient.scan(scanRequest);
            if (!areAllTestProductsPresent(items)) {
                throw new DataNotReadyException("sqs-batch-processing not complete yet");
            }
            return null;
        }, "sqs-batch-processing", DataNotReadyException.class).get();

        ScanResponse finalItems = ddbClient.scan(scanRequest);
        assertThat(areAllTestProductsPresent(finalItems)).isTrue();
    }

    @Test
    void kinesisBatchProcessingSucceeds() {
        List<PutRecordsRequestEntry> entries = testProducts.stream()
                .map(p -> {
                    try {
                        return PutRecordsRequestEntry.builder()
                                .partitionKey("1")
                                .data(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(p)))
                                .build();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        // WHEN
        kinesisClient.putRecords(PutRecordsRequest.builder()
                .streamName(kinesisStreamName)
                .records(entries)
                .build());

        // THEN
        ScanRequest scanRequest = ScanRequest.builder().tableName(outputTable).build();
        RetryUtils.withRetry(() -> {
            ScanResponse items = ddbClient.scan(scanRequest);
            if (!areAllTestProductsPresent(items)) {
                throw new DataNotReadyException("kinesis-batch-processing not complete yet");
            }
            return null;
        }, "kinesis-batch-processing", DataNotReadyException.class).get();

        ScanResponse finalItems = ddbClient.scan(scanRequest);
        assertThat(areAllTestProductsPresent(finalItems)).isTrue();
    }

    @Test
    void ddbStreamsBatchProcessingSucceeds() {
        // GIVEN
        String theId = "my-test-id";

        // WHEN
        ddbClient.putItem(PutItemRequest.builder()
                .tableName(ddbStreamsTestTable)
                .item(new HashMap<String, AttributeValue>() {
                    {
                        put("id", AttributeValue.builder()
                                .s(theId)
                                .build());
                    }
                })
                .build());

        // THEN
        ScanRequest scanRequest = ScanRequest.builder().tableName(outputTable).build();
        RetryUtils.withRetry(() -> {
            ScanResponse items = ddbClient.scan(scanRequest);
            if (items.count() != 1) {
                throw new DataNotReadyException("DDB streams processing not complete yet");
            }
            return null;
        }, "ddb-streams-batch-processing", DataNotReadyException.class).get();

        ScanResponse finalItems = ddbClient.scan(scanRequest);
        assertThat(finalItems.count()).isEqualTo(1);
        assertThat(finalItems.items().get(0).get("id").s()).isEqualTo(theId);
    }

    private boolean areAllTestProductsPresent(ScanResponse items) {
        for (Product p : testProducts) {
            boolean foundIt = false;
            for (Map<String, AttributeValue> a : items.items()) {
                if (a.get("id").s().equals(Long.toString(p.id))) {
                    foundIt = true;
                    break;
                }
            }
            if (!foundIt) {
                return false;
            }
        }
        return true;
    }

    class Product {
        private long id;

        private String name;

        private double price;

        public Product() {
        }

        public Product(long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }
    }
}
