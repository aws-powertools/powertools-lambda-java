package org.demo.batch.dynamo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.demo.batch.model.DdbProduct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.stream.IntStream;

public class DynamoDBWriter implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOGGER = LogManager.getLogger(DynamoDBWriter.class);

    private final DynamoDbEnhancedClient enhancedClient;

    private final SecureRandom random;

    public DynamoDBWriter() {
        random = new SecureRandom();
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {
        String tableName = System.getenv("TABLE_NAME");

        LOGGER.info("handleRequest");

        WriteBatch.Builder<DdbProduct> productBuilder = WriteBatch.builder(DdbProduct.class)
                .mappedTableResource(enhancedClient.table(tableName, TableSchema.fromBean(DdbProduct.class)));

        IntStream.range(0, 5).forEach(i -> {
            String id = UUID.randomUUID().toString();

            float price = random.nextFloat();
            DdbProduct product = new DdbProduct(id, "product-" + id, price);
            productBuilder.addPutItem(product);
        });

        BatchWriteItemEnhancedRequest batchWriteItemEnhancedRequest = BatchWriteItemEnhancedRequest.builder().writeBatches(
                productBuilder.build())
                .build();

        BatchWriteResult batchWriteResult = enhancedClient.batchWriteItem(batchWriteItemEnhancedRequest);


        LOGGER.info("Wrote batch of messages to DynamoDB: {}", batchWriteResult);

        return "Success";
    }
}
