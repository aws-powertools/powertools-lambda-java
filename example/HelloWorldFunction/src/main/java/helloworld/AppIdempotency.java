package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.persistence.DynamoDBPersistenceStore;
import software.amazon.lambda.powertools.utilities.JsonConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AppIdempotency implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final static Logger LOG = LogManager.getLogger();

    public AppIdempotency() {
        // we need to initialize idempotency configuration before the handleRequest method is called
        Idempotency.config().withConfig(
                        IdempotencyConfig.builder()
                                .withEventKeyJMESPath("powertools_json(body).address")
                                .build())
                .withPersistenceStore(
                        DynamoDBPersistenceStore.builder()
                                .withTableName("idempotency_table")
                                .build()
                ).configure();
    }


    /**
     * Try with:
     * <pre>
     *     curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/helloidem/ -H "Content-Type: application/json" -d '{"address": "https://checkip.amazonaws.com"}'
     * </pre>
     * @param input
     * @param context
     * @return
     */
    @Idempotent
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

            LOG.debug("ip is {}", pageContents);
            return response
                    .withStatusCode(200)
                    .withBody(output);

        } catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    // we could actually also put the @Idempotent annotation here
    private String getPageContents(String address) throws IOException {
        URL url = new URL(address);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
