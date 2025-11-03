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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import software.amazon.lambda.powertools.testutils.DataNotReadyException;
import software.amazon.lambda.powertools.testutils.Infrastructure;
import software.amazon.lambda.powertools.testutils.RetryUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LargeMessageE2ET {

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

    private Infrastructure infrastructure;
    private String functionName;
    private String bucketName;
    private String queueUrl;
    private String tableName;
    private String messageId;
    private String currentPathToFunction;

    private void setupInfrastructure(String pathToFunction) {
        // Do not re-deploy the same function
        if (pathToFunction.equals(currentPathToFunction)) {
            return;
        }

        // Destroy any existing infrastructure before re-deploying
        if (infrastructure != null) {
            infrastructure.destroy();
        }

        String random = UUID.randomUUID().toString().substring(0, 6);
        bucketName = "largemessagebucket" + random;
        String queueName = "largemessagequeue" + random;

        infrastructure = Infrastructure.builder()
                .testName(LargeMessageE2ET.class.getSimpleName() + "-" + pathToFunction)
                .queue(queueName)
                .largeMessagesBucket(bucketName)
                .pathToFunction(pathToFunction)
                .timeoutInSeconds(60)
                .build();

        Map<String, String> outputs = infrastructure.deploy();
        functionName = outputs.get(FUNCTION_NAME_OUTPUT);
        queueUrl = outputs.get("QueueURL");
        tableName = outputs.get("TableNameForAsyncTests");
        currentPathToFunction = pathToFunction;

        LOG.info("Testing '" + LargeMessageE2ET.class.getSimpleName() + "' with " + pathToFunction);
    }

    @AfterAll
    void cleanup() {
        if (infrastructure != null) {
            infrastructure.destroy();
        }
    }

    @AfterEach
    void tearDown() {
        reset();
    }

    private void reset() {
        if (messageId != null) {
            Map<String, AttributeValue> itemToDelete = new HashMap<>();
            itemToDelete.put("functionName", AttributeValue.builder().s(functionName).build());
            itemToDelete.put("id", AttributeValue.builder().s(messageId).build());
            dynamoDbClient.deleteItem(DeleteItemRequest.builder().tableName(tableName).key(itemToDelete).build());
            messageId = null;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "largemessage", "largemessage-functional" })
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void bigSQSMessageOffloadedToS3_shouldLoadFromS3(String pathToFunction) throws IOException {
        setupInfrastructure(pathToFunction);

        // GIVEN
        final ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(s3Client, bucketName);
        try (AmazonSQSExtendedClient client = new AmazonSQSExtendedClient(
                SqsClient.builder().region(region).httpClient(httpClient).build(), extendedClientConfig);
                InputStream inputStream = this.getClass().getResourceAsStream("/large_sqs_message.txt");) {
            String bigMessage = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            // WHEN
            client.sendMessage(SendMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .messageBody(bigMessage)
                    .build());
        }

        // THEN
        QueryRequest request = QueryRequest
                .builder()
                .tableName(tableName)
                .keyConditionExpression("functionName = :func")
                .expressionAttributeValues(
                        Collections.singletonMap(":func", AttributeValue.builder().s(functionName).build()))
                .build();

        RetryUtils.withRetry(() -> {
            QueryResponse response = dynamoDbClient.query(request);
            if (response.items().size() != 1) {
                throw new DataNotReadyException("Large message processing not complete yet");
            }
            return null;
        }, "large-message-processing", DataNotReadyException.class).get();

        QueryResponse finalResponse = dynamoDbClient.query(request);
        List<Map<String, AttributeValue>> items = finalResponse.items();
        assertThat(items).hasSize(1);
        messageId = items.get(0).get("id").s();
        assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(300977);
        assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("22bde5e7b05fa80bc7be45bdd4bc6c75");
    }

    @ParameterizedTest
    @ValueSource(strings = { "largemessage", "largemessage-functional" })
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void smallSQSMessage_shouldNotReadFromS3(String pathToFunction) {
        setupInfrastructure(pathToFunction);

        // GIVEN
        final ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration()
                .withPayloadSupportEnabled(s3Client, bucketName);
        try (AmazonSQSExtendedClient client = new AmazonSQSExtendedClient(
                SqsClient.builder().region(region).httpClient(httpClient).build(),
                extendedClientConfig)) {
            String message = "Hello World";

            // WHEN
            client.sendMessage(SendMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .build());

            // THEN
            QueryRequest request = QueryRequest
                    .builder()
                    .tableName(tableName)
                    .keyConditionExpression("functionName = :func")
                    .expressionAttributeValues(
                            Collections.singletonMap(":func", AttributeValue.builder().s(functionName).build()))
                    .build();

            RetryUtils.withRetry(() -> {
                QueryResponse response = dynamoDbClient.query(request);
                if (response.items().size() != 1) {
                    throw new DataNotReadyException("Small message processing not complete yet");
                }
                return null;
            }, "small-message-processing", DataNotReadyException.class).get();

            QueryResponse finalResponse = dynamoDbClient.query(request);
            List<Map<String, AttributeValue>> items = finalResponse.items();
            assertThat(items).hasSize(1);
            messageId = items.get(0).get("id").s();
            assertThat(Integer.valueOf(items.get(0).get("bodySize").n())).isEqualTo(
                    message.getBytes(StandardCharsets.UTF_8).length);
            assertThat(items.get(0).get("bodyMD5").s()).isEqualTo("b10a8db164e0754105b7a99be72e3fe5");
        }
    }
}
