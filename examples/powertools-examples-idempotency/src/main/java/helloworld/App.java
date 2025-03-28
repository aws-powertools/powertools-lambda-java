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

package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.IdempotencyConfig;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.persistence.dynamodb.DynamoDBPersistenceStore;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.utilities.JsonConfig;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public App() {
        this(null);
    }

    public App(DynamoDbClient client) {
        Idempotency.config().withConfig(
                IdempotencyConfig.builder()
                        .withEventKeyJMESPath("powertools_json(body).address")
                        .withResponseHook((responseData, dataRecord) -> {
                            if (responseData instanceof APIGatewayProxyResponseEvent) {
                                APIGatewayProxyResponseEvent proxyResponse = (APIGatewayProxyResponseEvent) responseData;
                                final Map<String, String> headers = new HashMap<>();
                                headers.putAll(proxyResponse.getHeaders());
                                headers.put("x-idempotency-response", "true");
                                headers.put("x-idempotency-expiration",
                                        String.valueOf(dataRecord.getExpiryTimestamp()));

                                proxyResponse.setHeaders(headers);

                                return proxyResponse;
                            }

                            return responseData;
                        })
                        .build())
                .withPersistenceStore(
                        DynamoDBPersistenceStore.builder()
                                .withDynamoDbClient(client)
                                .withTableName(System.getenv("IDEMPOTENCY_TABLE"))
                                .build())
                .configure();
    }

    /**
     * This is your Lambda event handler. It accepts HTTP POST requests from API gateway and returns the contents of the
     * given URL. Requests are made idempotent
     * by the idempotency library, and results are cached for the default 1h expiry time.
     * <p>
     * You can test the endpoint like this:
     *
     * <pre>
     *     curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/helloidem/ -H "Content-Type: application/json" -d '{"address": "https://checkip.amazonaws.com"}'
     * </pre>
     * <ul>
     * <li>First call will execute the handleRequest normally, and store the response in the idempotency table (Look
     * into DynamoDB)</li>
     * <li>Second call (and next ones) will retrieve from the cache (if cache is enabled, which is by default) or from
     * the store, the handler won't be called. Until the expiration happens (by default 1 hour).</li>
     * </ul>
     */
    @Idempotent // The magic is here!
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
            // Read the 'address' field from the JSON post body
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

    /**
     * Helper to retrieve the contents of the given URL and return them as a string.
     * <p>
     * We could also put the @Idempotent annotation here if we only wanted this sub-operation to be idempotent. Putting
     * it on the handler, however, reduces total execution time and saves us time!
     *
     * @param address
     *            The URL to fetch
     * @return The contents of the given URL
     * @throws IOException
     */
    private String getPageContents(String address) throws IOException {
        URL url = new URL(address);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
