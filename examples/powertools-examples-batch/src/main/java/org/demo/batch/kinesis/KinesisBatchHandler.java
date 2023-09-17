package org.demo.batch.kinesis;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.demo.batch.model.Product;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

public class KinesisBatchHandler implements RequestHandler<KinesisEvent, StreamsEventResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisBatchHandler.class);
    private final BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler;

    public KinesisBatchHandler() {
        handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .buildWithMessageHandler(this::processMessage, Product.class);
    }

    @Override
    public StreamsEventResponse handleRequest(KinesisEvent kinesisEvent, Context context) {
        return handler.processBatch(kinesisEvent, context);
    }

    private void processMessage(Product p, Context c) {
        LOGGER.info("Processing product " + p);
    }

}
