package org.demo.batch.dynamo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.demo.batch.model.DdbProduct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDBWriter implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBWriter.class);

    private final DynamoDbEnhancedClient enhancedClient;

    private final SecureRandom random;

    public DynamoDBWriter() {
        random = new SecureRandom();
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .httpClientBuilder(AwsCrtHttpClient.builder())
                .build();

        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Override
    public String handleRequest(ScheduledEvent scheduledEvent, Context context) {
        String tableName = System.getenv("TABLE_NAME");

        LOGGER.info("handleRequest");

        List<DdbProduct> products = createProducts(tableName);
        List<DdbProduct> updatedProducts = updateProducts(tableName, products);
        deleteProducts(tableName, updatedProducts);

        return "Success";
    }

    private void deleteProducts(String tableName, List<DdbProduct> updatedProducts) {
        WriteBatch.Builder<DdbProduct> productDeleteBuilder = WriteBatch.builder(DdbProduct.class)
                .mappedTableResource(enhancedClient.table(tableName, TableSchema.fromBean(DdbProduct.class)));

        updatedProducts.forEach(productDeleteBuilder::addDeleteItem);

        BatchWriteResult batchDeleteResult = enhancedClient
                .batchWriteItem(BatchWriteItemEnhancedRequest.builder().writeBatches(
                        productDeleteBuilder.build())
                .build());
        LOGGER.info("Deleted batch of objects from DynamoDB: {}", batchDeleteResult);
    }

    private List<DdbProduct> updateProducts(String tableName, List<DdbProduct> products) {
        WriteBatch.Builder<DdbProduct> productUpdateBuilder = WriteBatch.builder(DdbProduct.class)
                .mappedTableResource(enhancedClient.table(tableName, TableSchema.fromBean(DdbProduct.class)));

        List<DdbProduct> updatedProducts = products.stream().map(product -> {
            // Update the price of the product and add it to the batch
            LOGGER.info("Updating product: {}", product);
            float price = random.nextFloat();
            DdbProduct updatedProduct = new DdbProduct(product.getId(), "updated-product-" + product.getId(), price);
            productUpdateBuilder.addPutItem(updatedProduct);
            return updatedProduct;
        }).collect(Collectors.toList());

        BatchWriteResult batchUpdateResult = enhancedClient
                .batchWriteItem(BatchWriteItemEnhancedRequest.builder().writeBatches(
                        productUpdateBuilder.build())
                .build());
        LOGGER.info("Updated batch of objects to DynamoDB: {}", batchUpdateResult);
        return updatedProducts;
    }

    public List<DdbProduct> createProducts(String tableName) {
        WriteBatch.Builder<DdbProduct> productBuilder = WriteBatch.builder(DdbProduct.class)
                .mappedTableResource(enhancedClient.table(tableName, TableSchema.fromBean(DdbProduct.class)));

        List<DdbProduct> ddbProductStream = IntStream.range(0, 5).mapToObj(i -> {
            String id = UUID.randomUUID().toString();
            float price = random.nextFloat();
            // Create a new product and add it to the batch
            final DdbProduct product = new DdbProduct(id, "product-" + id, price);
            productBuilder.addPutItem(product);
            return product;
        }).collect(Collectors.toList());

        BatchWriteResult batchWriteResult = enhancedClient
                .batchWriteItem(BatchWriteItemEnhancedRequest.builder().writeBatches(
                                productBuilder.build())
                        .build());
        LOGGER.info("Wrote batch of objects to DynamoDB: {}", batchWriteResult);
        return ddbProductStream;
    }
}
