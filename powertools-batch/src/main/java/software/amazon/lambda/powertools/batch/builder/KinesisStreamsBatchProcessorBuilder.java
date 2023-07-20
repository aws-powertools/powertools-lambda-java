package software.amazon.lambda.powertools.batch.builder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.handler.KinesisStreamsBatchMessageHandler;

import java.util.function.BiConsumer;

public class KinesisStreamsBatchProcessorBuilder extends AbstractBatchMessageHandlerBuilder<KinesisEvent.KinesisEventRecord,
        KinesisStreamsBatchProcessorBuilder,
        KinesisEvent,
        StreamsEventResponse>
{
    @Override
    public BatchMessageHandler<KinesisEvent, StreamsEventResponse> buildWithRawMessageHandler(BiConsumer<KinesisEvent.KinesisEventRecord, Context> rawMessageHandler) {
        return new KinesisStreamsBatchMessageHandler<Void>(
            rawMessageHandler,
            null,
            null,
            successHandler,
            failureHandler);
    }

    @Override
    public <M> BatchMessageHandler<KinesisEvent, StreamsEventResponse> buildWithMessageHandler(BiConsumer<M, Context> messageHandler, Class<M> messageClass) {
        return new KinesisStreamsBatchMessageHandler<>(
                null,
                messageHandler,
                messageClass,
                successHandler,
                failureHandler);
    }

    @Override
    protected KinesisStreamsBatchProcessorBuilder getThis() {
        return null;
    }
}
