package org.demo.batch.dynamo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DynamoDBStreamBatchHandlerParallel implements RequestHandler<DynamodbEvent, StreamsEventResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBStreamBatchHandlerParallel.class);
    private final BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler;
    private final ExecutorService executor;

    public DynamoDBStreamBatchHandlerParallel() {
        handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessage);
        executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent ddbEvent, Context context) {
        return handler.processBatchInParallel(ddbEvent, context, executor);
    }

    private void processMessage(DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord) {
        LOGGER.info("Processing DynamoDB Stream Record" + dynamodbStreamRecord);
    }

}
