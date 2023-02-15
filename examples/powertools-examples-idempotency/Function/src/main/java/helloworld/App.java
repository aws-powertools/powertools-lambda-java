package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.persistence.DynamoDBPersistenceStore;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.utilities.JsonConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final static Logger log = LogManager.getLogger(App.class);

    public App() {
        this(null);
    }

    public App(DynamoDbClient client) {
        Idempotency.config().withConfig(
                        IdempotencyConfig.builder()
                                .withEventKeyJMESPath("powertools_json(body).address") // will retrieve the address field in the body which is a string transformed to json with `powertools_json`
                                .build())
                .withPersistenceStore(
                        DynamoDBPersistenceStore.builder()
                                .withDynamoDbClient(client)
                                .withTableName(System.getenv("IDEMPOTENCY_TABLE"))
                                .build()
                ).configure();
    }

    /**
     * Try with:
     * <pre>
     *     curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/helloidem/ -H "Content-Type: application/json" -d '{"address": "https://checkip.amazonaws.com"}'
     * </pre>
     * <ul>
     *     <li>First call will execute the handleRequest normally, and store the response in the idempotency table (Look into DynamoDB)</li>
     *     <li>Second call (and next ones) will retrieve from the cache (if cache is enabled, which is by default) or from the store, the handler won't be called. Until the expiration happens (by default 1 hour).</li>
     * </ul>
     */
    @Idempotent // *** THE MAGIC IS HERE ***
    @Logging(logEvent = true)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "*");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            String address = JsonConfig.get().getObjectMapper().readTree(input.getBody()).get("address").asText();
            final String pageContents = this.getPageContents(address);
            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);

            log.info("ip is {}", pageContents);
            return response
                    .withStatusCode(200)
                    .withBody(output);

        } catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    // we could also put the @Idempotent annotation here, but using it on the handler avoids executing the handler (cost reduction).
    // Use it on other methods to handle multiple items (with SQS batch processing for example)
    private String getPageContents(String address) throws IOException {
        URL url = new URL(address);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}