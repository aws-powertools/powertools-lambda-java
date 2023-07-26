package org.demo.serialization;

import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class APIGatewayRequestDeserializationFunction
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final static Logger LOGGER = LogManager.getLogger(APIGatewayRequestDeserializationFunction.class);
    private static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put("Content-Type", "application/json");
        put("X-Custom-Header", "application/json");
    }};

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        Product product = extractDataFrom(event).as(Product.class);
        LOGGER.info("\n=============== Deserialized request body: ===============");
        LOGGER.info("product={}\n", product);

        return new APIGatewayProxyResponseEvent()
                .withHeaders(HEADERS)
                .withStatusCode(200)
                .withBody("Received request for productId: " + product.getId());
    }
}

