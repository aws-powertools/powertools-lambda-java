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
import static software.amazon.lambda.powertools.testutils.Infrastructure.FUNCTION_NAME_OUTPUT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.utilities.JsonConfig;

public class BatchE2ET {
    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
    private static Infrastructure infrastructure;
    private static String functionName;
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
                new Product(3, "product3", 6.78)
        );
    }

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
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
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
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
    public static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @AfterEach
    public void cleanUpTest() {
        // Delete everything in the output table
        ScanResponse items = ddbClient.scan(ScanRequest.builder()
                .tableName(outputTable)
                .build());

        for (Map<String, AttributeValue> item : items.items()) {
            HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>() {
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
    public void sqsBatchProcessingSucceeds() throws InterruptedException {
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
        Thread.sleep(30000); // wait for function to be executed

        // THEN
        ScanResponse items = ddbClient.scan(ScanRequest.builder()
                .tableName(outputTable)
                .build());
        validateAllItemsHandled(items);
    }

    @Test
    public void kinesisBatchProcessingSucceeds() throws InterruptedException {
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
        PutRecordsResponse result = kinesisClient.putRecords(PutRecordsRequest.builder()
                .streamName(kinesisStreamName)
                .records(entries)
                .build());
        Thread.sleep(30000); // wait for function to be executed

        // THEN
        ScanResponse items = ddbClient.scan(ScanRequest.builder()
                .tableName(outputTable)
                .build());
        validateAllItemsHandled(items);
    }

    @Test
    public void ddbStreamsBatchProcessingSucceeds() throws InterruptedException {
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
        Thread.sleep(90000); // wait for function to be executed

        // THEN
        ScanResponse items = ddbClient.scan(ScanRequest.builder()
                .tableName(outputTable)
                .build());

        assertThat(items.count()).isEqualTo(1);
        assertThat(items.items().get(0).get("id").s()).isEqualTo(theId);
    }

    private void validateAllItemsHandled(ScanResponse items) {
        for (Product p : testProducts) {
            boolean foundIt = false;
            for (Map<String, AttributeValue> a : items.items()) {
                if (a.get("id").s().equals(Long.toString(p.id))) {
                    foundIt = true;
                }
            }
            assertThat(foundIt).isTrue();
        }
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
