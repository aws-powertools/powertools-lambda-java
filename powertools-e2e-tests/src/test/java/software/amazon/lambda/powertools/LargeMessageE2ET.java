package software.amazon.lambda.powertools;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.lambda.powertools.testutils.Infrastructure.FUNCTION_NAME_OUTPUT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.lambda.powertools.testutils.Infrastructure;

public class LargeMessageE2ET {

    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageE2ET.class);
    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));

    private static final S3Client s3Client = S3Client.builder()
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
    private String messageId;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public static void setup() {
        String random = UUID.randomUUID().toString().substring(0, 6);
        bucketName = "largemessagebucket" + random;
        String queueName = "largemessagequeue" + random;

        infrastructure = Infrastructure.builder()
                .testName(LargeMessageE2ET.class.getSimpleName())
                .queue(queueName)
                .largeMessagesBucket(bucketName)
                .pathToFunction("largemessage")
                .timeoutInSeconds(60)
                .build();

        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
        queueUrl = outputs.get("QueueURL");
        tableName = outputs.get("TableNameForAsyncTests");

        LOG.info("Testing '" + LargeMessageE2ET.class.getSimpleName() + "'");
    }

    @AfterAll
    public static void tearDown() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @AfterEach
    public void reset() {
        if (messageId != null) {
            Map<String, AttributeValue> itemToDelete = new HashMap<>();
            itemToDelete.put("functionName", AttributeValue.builder().s(functionName).build());
            itemToDelete.put("id", AttributeValue.builder().s(messageId).build());
            dynamoDbClient.deleteItem(DeleteItemRequest.builder().tableName(tableName).key(itemToDelete).build());
            messageId = null;
        }
    }

    @Test
    public void bigSQSMessageOffloadedToS3_shouldLoadFromS3() throws IOException, InterruptedException {
        // given
        final ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(s3Client, bucketName);
        try (AmazonSQSExtendedClient client = new AmazonSQSExtendedClient(
                SqsClient.builder().region(region).httpClient(httpClient).build(), extendedClientConfig)) {
            InputStream inputStream = this.getClass().getResourceAsStream("/large_sqs_message.txt");
            String bigMessage = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            // when
            client.sendMessage(SendMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .messageBody(bigMessage)
                    .build());
        }
        Thread.sleep(30000); // wait for function to be executed

        // then
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
        messageId = items.get(0).get("id").s();
        assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(300977);
        assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("22bde5e7b05fa80bc7be45bdd4bc6c75");
    }

    @Test
    public void smallSQSMessage_shouldNotReadFromS3() throws IOException, InterruptedException {
        // given
        final ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(s3Client, bucketName);
        try (AmazonSQSExtendedClient client = new AmazonSQSExtendedClient(
                SqsClient.builder().region(region).httpClient(httpClient).build(),
                extendedClientConfig)) {
            String message = "Hello World";

            // when
            client.sendMessage(SendMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .build());

            Thread.sleep(30000); // wait for function to be executed

            // then
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
            messageId = items.get(0).get("id").s();
            assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(
                    message.getBytes(StandardCharsets.UTF_8).length);
            assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("b10a8db164e0754105b7a99be72e3fe5");
        }
    }
}
