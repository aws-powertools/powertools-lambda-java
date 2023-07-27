package software.amazon.lambda.powertools.batch.builder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import java.util.function.BiConsumer;
import software.amazon.lambda.powertools.batch.exception.DeserializationNotSupportedException;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.handler.DynamoDbBatchMessageHandler;

/**
 * Builds a batch processor for processing DynamoDB Streams batch events
 **/
public class DynamoDbBatchMessageHandlerBuilder
        extends AbstractBatchMessageHandlerBuilder<DynamodbEvent.DynamodbStreamRecord,
        DynamoDbBatchMessageHandlerBuilder,
        DynamodbEvent,
        StreamsEventResponse> {


    @Override
    public BatchMessageHandler<DynamodbEvent, StreamsEventResponse> buildWithRawMessageHandler(
            BiConsumer<DynamodbEvent.DynamodbStreamRecord, Context> rawMessageHandler) {
        return new DynamoDbBatchMessageHandler(
                this.successHandler,
                this.failureHandler,
                rawMessageHandler);
    }

    @Override
    public <M> BatchMessageHandler<DynamodbEvent, StreamsEventResponse> buildWithMessageHandler(
            BiConsumer<M, Context> handler, Class<M> messageClass) {
        // The DDB provider streams DynamoDB changes, and therefore does not have a customizable payload
        throw new DeserializationNotSupportedException();
    }

    @Override
    protected DynamoDbBatchMessageHandlerBuilder getThis() {
        return this;
    }
}
