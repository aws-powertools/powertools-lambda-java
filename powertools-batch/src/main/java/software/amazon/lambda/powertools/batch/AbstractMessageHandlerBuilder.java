package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * An abstract class to capture common arguments used
 * across all the message-binding-specific builders.
 *
 * @param <T> The type of a single message in the batch
 * @param <C> The type of the child builder
 * @param <E> The type of the Lambda event
 * @param <R> The type of the batch response
 */
abstract class AbstractMessageHandlerBuilder<T, C, E, R> {
    protected BiConsumer<SQSEvent.SQSMessage, Exception> failureHandler;
    protected Consumer<SQSEvent.SQSMessage> successHandler;
    protected BiConsumer<?, Context> messageHandler;
    protected BiConsumer<T, Context> rawMessageHandler;


    public C withSuccessHandler(Consumer<SQSEvent.SQSMessage> handler) {
        this.successHandler = handler;
        return getThis();
    }

    public C withFailureHandler(BiConsumer<SQSEvent.SQSMessage, Exception> handler) {
        this.failureHandler = handler;
        return getThis();
    }

    public <M> C withMessageHandler(BiConsumer<M, Context> handler) {
        if (this.rawMessageHandler != null) {
            throw new TooManyMessageHandlersException();
        }

        this.messageHandler = handler;
        return getThis();
    }

    public C withRawMessageHandler(BiConsumer<T, Context> handler) {
        if (this.messageHandler != null) {
            throw new TooManyMessageHandlersException();
        }

        this.rawMessageHandler = handler;
        return getThis();
    }

    public abstract BatchMessageHandler<E, R> build();


        protected abstract C getThis();
}
