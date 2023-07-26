package org.demo.serialization;

import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SQSEventDeserializationFunction implements RequestHandler<SQSEvent, String> {

    private final static Logger LOGGER = LogManager.getLogger(SQSEventDeserializationFunction.class);

    public String handleRequest(SQSEvent event, Context context) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);

        LOGGER.info("\n=============== Deserialized messages: ===============");
        LOGGER.info("products={}\n", products);

        return "Number of received messages: " + products.size();
    }
}

