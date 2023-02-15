package org.demo.parameters;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
import static software.amazon.lambda.powertools.parameters.transform.Transformer.json;

public class ParametersFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final static Logger log = LogManager.getLogger();

    SSMProvider ssmProvider = ParamManager.getSsmProvider();
    SecretsProvider secretsProvider = ParamManager.getSecretsProvider();

    String simpleValue = ssmProvider.defaultMaxAge(30, SECONDS).get("/powertools-java/sample/simplekey");
    String listValue = ssmProvider.withMaxAge(60, SECONDS).get("/powertools-java/sample/keylist");
    MyObject jsonObj = ssmProvider.withTransformation(json).get("/powertools-java/sample/keyjson", MyObject.class);
    Map<String, String> allValues = ssmProvider.getMultiple("/powertools-java/sample");
    String b64value = ssmProvider.withTransformation(base64).get("/powertools-java/sample/keybase64");

    Map<String, String> secretJson = secretsProvider.withTransformation(json).get("/powertools-java/userpwd", Map.class);
    MyObject secretJsonObj = secretsProvider.withMaxAge(42, SECONDS).withTransformation(json).get("/powertools-java/secretcode", MyObject.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        log.info("\n=============== SSM Parameter Store ===============");
        log.info("simplevalue={}, listvalue={}, b64value={}\n", simpleValue, listValue, b64value);
        log.info("jsonobj={}\n", jsonObj);

        log.info("allvalues (multiple):");
        allValues.forEach((key, value) -> log.info("- {}={}\n", key, value));

        log.info("\n=============== Secrets Manager ===============");
        log.info("secretjson:");
        secretJson.forEach((key, value) -> log.info("- {}={}\n", key, value));
        log.info("secretjsonobj={}\n", secretJsonObj);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private String getPageContents(String address) throws IOException{
        URL url = new URL(address);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
