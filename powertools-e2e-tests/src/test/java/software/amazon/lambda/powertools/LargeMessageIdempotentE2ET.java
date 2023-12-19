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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.lambda.powertools.testutils.Infrastructure;

public class LargeMessageIdempotentE2ET {

    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageIdempotentE2ET.class);
    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));

    private static final S3Client s3Client = S3Client.builder()
            .httpClient(httpClient)
            .region(region)
            .build();
    // cannot use the extended library as it will create different S3 objects (we need to have the same for Idempotency)
    private static final SqsClient sqsClient = SqsClient.builder()
            .httpClient(httpClient)
            .region(region)
            .build();
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(httpClient)
            .region(region)
            .build();

    private static Infrastructure infrastructure;
    private static String functionName;
    private static String bucketName;
    private static String queueUrl;
    private static String tableName;


    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        String random = UUID.randomUUID().toString().substring(0, 6);
        bucketName = "largemessagebucket" + random;
        String queueName = "largemessagequeue" + random;

        infrastructure = Infrastructure.builder()
                .testName(LargeMessageIdempotentE2ET.class.getSimpleName())
                .pathToFunction("largemessage_idempotent")
                .idempotencyTable("idempo" + random)
                .queue(queueName)
                .largeMessagesBucket(bucketName)
                .build();

        Map<String, String> outputs = infrastructure.deploy();

        functionName = outputs.get(Infrastructure.FUNCTION_NAME_OUTPUT);
        queueUrl = outputs.get("QueueURL");
        tableName = outputs.get("TableNameForAsyncTests");

        LOG.info("Testing '" + LargeMessageIdempotentE2ET.class.getSimpleName() + "'");
    }

    @AfterAll
    public static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @Test
    public void test_ttlNotExpired_doesNotInsertInDDB_ttlExpired_insertInDDB() throws InterruptedException,
            IOException {
        int waitMs = 15000;

        // GIVEN
        InputStream inputStream = this.getClass().getResourceAsStream("/large_sqs_message.txt");
        String bigMessage = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        // upload manually to S3
        String key = UUID.randomUUID().toString();
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build(), RequestBody.fromString(bigMessage));

        // WHEN
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(String.format(
                        "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"%s\",\"s3Key\":\"%s\"}]",
                        bucketName, key))
                .messageAttributes(Collections.singletonMap("SQSLargePayloadSize", MessageAttributeValue.builder()
                        .stringValue("300977")
                        .dataType("Number")
                        .build()))
                .build();

        // First invocation
        // send message to SQS with the good pointer and metadata
        sqsClient.sendMessage(messageRequest);
        Thread.sleep(waitMs); // wait for the function to be invoked & executed

        // THEN
        QueryRequest request = QueryRequest
                .builder()
                .tableName(tableName)
                .keyConditionExpression("functionName = :func")
                .expressionAttributeValues(
                        Collections.singletonMap(":func", AttributeValue.builder().s(functionName).build()))
                .build();
        QueryResponse response = dynamoDbClient.query(request);
        List<Map<String, AttributeValue>> items = response.items();
        assertThat(items).hasSize(1);
        assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(300977);
        assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("22bde5e7b05fa80bc7be45bdd4bc6c75");
        long timeOfInvocation1 = Long.parseLong(items.get(0).get("now").n());

        // WHEN
        // Second invocation
        // send the same message before ttl expires
        sqsClient.sendMessage(messageRequest);
        Thread.sleep(waitMs); // wait for the function to be invoked & executed

        // THEN
        response = dynamoDbClient.query(request);
        items = response.items();
        assertThat(items).hasSize(1); // we should have the same number of items (idempotency working)
        assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(300977);
        assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("22bde5e7b05fa80bc7be45bdd4bc6c75");
        long timeOfInvocation2 = Long.parseLong(items.get(0).get("now").n());
        assertThat(timeOfInvocation2).isEqualTo(timeOfInvocation1); // should be the same as first invocation

        // WHEN
        // waiting for TTL to expire
        Thread.sleep(24000);

        // Third invocation
        // send the same message again
        sqsClient.sendMessage(messageRequest);
        Thread.sleep(waitMs); // wait for the function to be invoked

        // THEN
        response = dynamoDbClient.query(request);
        items = response.items();
        assertThat(items).hasSize(2); // not idempotent anymore, function should put a new item in DDB
        assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(300977);
        assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("22bde5e7b05fa80bc7be45bdd4bc6c75");
        assertThat(Integer.valueOf(items.get(1).get("bodySize").n())).isEqualTo(300977);
        assertThat(items.get(1).get("bodyMD5").s()).isEqualTo("22bde5e7b05fa80bc7be45bdd4bc6c75");
        long timeOfInvocation3 = Long.parseLong(items.get(0).get("now").n());
        long timeOfInvocation4 = Long.parseLong(items.get(1).get("now").n());
        assertThat(timeOfInvocation3).isNotEqualTo(timeOfInvocation4); // should be different (not idempotent anymore)

    }
}
