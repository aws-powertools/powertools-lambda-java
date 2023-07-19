package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.handler.SqsBatchMessageHandler;

/**
 * Builds a batch processor for the SQS event source.
 */
public class SqsBatchMessageHandlerBuilder extends AbstractMessageHandlerBuilder<SQSEvent.SQSMessage,
        SqsBatchMessageHandlerBuilder,
        SQSEvent,
        SQSBatchResponse> {

    @Override
    protected SqsBatchMessageHandlerBuilder getThis() {
        return this;
    }

    @Override
    public BatchMessageHandler<SQSEvent, SQSBatchResponse> build() {

        return new SqsBatchMessageHandler(
                messageHandler,
                rawMessageHandler,
                successHandler,
                failureHandler
        );
    }
}
