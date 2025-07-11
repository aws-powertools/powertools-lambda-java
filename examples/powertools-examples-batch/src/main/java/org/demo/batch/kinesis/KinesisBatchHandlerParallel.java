package org.demo.batch.kinesis;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import org.demo.batch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KinesisBatchHandlerParallel implements RequestHandler<KinesisEvent, StreamsEventResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisBatchHandlerParallel.class);
    private final BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler;
    private final ExecutorService executor;


    public KinesisBatchHandlerParallel() {
        handler = new BatchMessageHandlerBuilder()
                .withKinesisBatchHandler()
                .buildWithMessageHandler(this::processMessage, Product.class);
        executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public StreamsEventResponse handleRequest(KinesisEvent kinesisEvent, Context context) {
        return handler.processBatchInParallel(kinesisEvent, context, executor);
    }

    private void processMessage(Product p) {
        LOGGER.info("Processing product " + p);
    }

}
