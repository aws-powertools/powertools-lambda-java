package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DdbBatchMessageHandler implements BatchMessageHandler<DynamodbEvent, StreamsEventResponse>{

    private final Consumer<DynamodbEvent.DynamodbStreamRecord> successHandler;
    private final BiConsumer<DynamodbEvent.DynamodbStreamRecord, Throwable> failureHandler;
    private final BiConsumer<DynamodbEvent.DynamodbStreamRecord, Context> rawMessageHandler;

    public DdbBatchMessageHandler(Consumer<DynamodbEvent.DynamodbStreamRecord> successHandler, BiConsumer<DynamodbEvent.DynamodbStreamRecord, Throwable> failureHandler, BiConsumer<DynamodbEvent.DynamodbStreamRecord, Context> rawMessageHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.rawMessageHandler = rawMessageHandler;
    }

    @Override
    public StreamsEventResponse processBatch(DynamodbEvent event, Context context) {
        // TODO Probably helpful https://github.com/aws-powertools/powertools-lambda-java/blob/ee64d620aff083b1a9734d880af2e077d73692a8/powertools-batch/src/main/java/software/amazon/lambda/powertools/batch2/DynamoDBBatchProcessor.java
        // TODO -success/failure handlers too
        throw new RuntimeException("Not implemented");
    }
}
