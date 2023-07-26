package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.persistence.DynamoDBPersistenceStore;


public class Function implements RequestHandler<Input, String> {

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
                                .withExpiration(Duration.of(10, ChronoUnit.SECONDS))
                                .build())
                .withPersistenceStore(
                        DynamoDBPersistenceStore.builder()
                                .withDynamoDbClient(client)
                                .withTableName(System.getenv("IDEMPOTENCY_TABLE"))
                                .build()
                ).configure();
    }

    @Idempotent
    public String handleRequest(Input input, Context context) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME.withZone(TimeZone.getTimeZone("UTC").toZoneId());
        return dtf.format(Instant.now());
    }
}