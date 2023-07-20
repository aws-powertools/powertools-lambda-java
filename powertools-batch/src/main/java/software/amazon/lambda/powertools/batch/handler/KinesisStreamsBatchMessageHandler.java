package software.amazon.lambda.powertools.batch.handler;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KinesisStreamsBatchMessageHandler <M> implements BatchMessageHandler<KinesisEvent, StreamsEventResponse> {

    private final BiConsumer<KinesisEvent.KinesisEventRecord, Context> rawMessageHandler;
    private final BiConsumer<M, Context> messageHandler;
    private final Class<M> messageClass;
    private final Consumer<KinesisEvent.KinesisEventRecord> successHandler;
    private final BiConsumer<KinesisEvent.KinesisEventRecord, Throwable> failureHandler;

    public KinesisStreamsBatchMessageHandler(BiConsumer<KinesisEvent.KinesisEventRecord, Context> rawMessageHandler,
                                                 BiConsumer<M, Context> messageHandler,
                                                 Class<M> messageClass, Consumer<KinesisEvent.KinesisEventRecord> successHandler,
                                                 BiConsumer<KinesisEvent.KinesisEventRecord, Throwable> failureHandler) {

        this.rawMessageHandler = rawMessageHandler;
        this.messageHandler = messageHandler;
        this.messageClass = messageClass;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public StreamsEventResponse processBatch(KinesisEvent event, Context context) {
        throw new RuntimeException("Not implemented");
    }
}
