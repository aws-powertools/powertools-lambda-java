package org.demo.batch.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.demo.batch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqsBatchHandlerParallel extends AbstractSqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqsBatchHandlerParallel.class);
    private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;
    private final ExecutorService executor;

    public SqsBatchHandlerParallel() {
        handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessage, Product.class);
        executor = Executors.newFixedThreadPool(2);
    }

    @Logging
    @Tracing
    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        LOGGER.info("Processing batch of {} messages", sqsEvent.getRecords().size());
        return handler.processBatchInParallel(sqsEvent, context, executor);
    }
}
