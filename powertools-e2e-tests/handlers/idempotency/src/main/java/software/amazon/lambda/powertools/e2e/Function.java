package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.persistence.DynamoDBPersistenceStore;

import java.time.Instant;
import java.time.format.DateTimeFormatter;


public class Function implements RequestHandler<String, String> {

    public Function() {
        this(DynamoDbClient
                .builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .region(Region.of(System.getenv("AWS_REGION")))
                .build());
    }

    public Function(DynamoDbClient client) {
        Idempotency.config().withConfig(
                        IdempotencyConfig.builder()
                                .build())
                .withPersistenceStore(
                        DynamoDBPersistenceStore.builder()
                                .withDynamoDbClient(client)
                                .withTableName(System.getenv("IDEMPOTENCY_TABLE"))
                                .build()
                ).configure();
    }

    @Idempotent
    public String handleRequest(String input, Context context) {
        DateTimeFormatter dtf =  DateTimeFormatter.ISO_DATE;
        return dtf.format(Instant.now());
    }
}