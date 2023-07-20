package software.amazon.lambda.powertools.batch.builder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import software.amazon.lambda.powertools.batch.exception.DeserializationNotSupportedException;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.handler.DdbBatchMessageHandler;

import java.util.function.BiConsumer;

public class DdbBatchMessageHandlerBuilder extends AbstractBatchMessageHandlerBuilder<DynamodbEvent.DynamodbStreamRecord,
        DdbBatchMessageHandlerBuilder,
        DynamodbEvent,
        StreamsEventResponse> {


    @Override
    public BatchMessageHandler<DynamodbEvent, StreamsEventResponse> buildWithRawMessageHandler(BiConsumer<DynamodbEvent.DynamodbStreamRecord, Context> rawMessageHandler) {
        return new DdbBatchMessageHandler(
                this.successHandler,
                this.failureHandler,
                rawMessageHandler);
    }

    @Override
    public <M> BatchMessageHandler<DynamodbEvent, StreamsEventResponse> buildWithMessageHandler(BiConsumer<M, Context> handler, Class<M> messageClass) {
        // The DDB provider streams DynamoDB changes, and therefore does not have a customizable payload
        throw new DeserializationNotSupportedException();
    }

    @Override
    protected DdbBatchMessageHandlerBuilder getThis() {
        return this;
    }
}
