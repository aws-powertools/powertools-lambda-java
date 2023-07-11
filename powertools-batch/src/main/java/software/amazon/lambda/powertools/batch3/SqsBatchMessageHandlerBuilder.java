package software.amazon.lambda.powertools.batch3;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builds a batch processor for the SQS event source.
 */
public class SqsBatchMessageHandlerBuilder {

    private Consumer<SQSEvent.SQSMessage> failureHandler;
    private Consumer<SQSEvent.SQSMessage> successHandler;

    /**
     * success and failure handler hooks are used as an example of a tuneable from the python
     * powertools.
     *
     * https://docs.powertools.aws.dev/lambda/python/2.15.0/utilities/batch/#extending-batchprocessor
     *
     * @param handler
     * @return
     */
    public SqsBatchMessageHandlerBuilder withSuccessHandler(Consumer<SQSEvent.SQSMessage> handler) {
        this.successHandler = handler;
        return this;
    }

    public SqsBatchMessageHandlerBuilder withFailureHandler(Consumer<SQSEvent.SQSMessage> handler) {
        this.failureHandler = handler;
        return this;
    }

    /**
     * The user can consume either raw messages ....
     */
    public <T> SQSBatchResponse processMessage(SQSEvent event, BiConsumer<T, Context> handler) {
        throw new NotImplementedException();
    }

    /**
     * ... or deserialized messages
     */
    public <T> SQSBatchResponse processRawMessage(SQSEvent event, BiConsumer<SQSEvent.SQSMessage, Context> handler) {
        throw new NotImplementedException();
    }
}
