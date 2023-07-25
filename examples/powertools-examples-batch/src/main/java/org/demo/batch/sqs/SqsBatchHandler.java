package org.demo.batch.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.demo.batch.model.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

public class SqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
        private final static Logger LOGGER = LogManager.getLogger(SqsBatchHandler.class);
    private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;

    public SqsBatchHandler() {
        handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessage, Product.class);
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        return handler.processBatch(sqsEvent, context);
    }


    private void processMessage(Product p, Context c) {
        LOGGER.info("Processing product " + p);
    }

}
