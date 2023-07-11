package software.amazon.lambda.powertools.batch3;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.function.Consumer;

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

    public <T> SQSBatchResponse processMessage(SQSEvent event, Consumer<T> handler) {
        throw new NotImplementedException();
    }

    public <T> SQSBatchResponse processRawMessage(SQSEvent event, Consumer<SQSEvent.SQSMessage> handler) {
        throw new NotImplementedException();
    }
}
