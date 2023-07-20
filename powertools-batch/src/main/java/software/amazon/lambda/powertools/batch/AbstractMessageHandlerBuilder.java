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
    protected BiConsumer<SQSEvent.SQSMessage, Throwable> failureHandler;
    protected Consumer<SQSEvent.SQSMessage> successHandler;

    public C withSuccessHandler(Consumer<SQSEvent.SQSMessage> handler) {
        this.successHandler = handler;
        return getThis();
    }

    public C withFailureHandler(BiConsumer<SQSEvent.SQSMessage, Throwable> handler) {
        this.failureHandler = handler;
        return getThis();
    }

    public abstract BatchMessageHandler<E, R> buildWithRawMessageHandler(BiConsumer<T, Context> handler);

    public BatchMessageHandler<E, R> buildWithRawMessageHandler(Consumer<T> handler) {
        return buildWithRawMessageHandler((f, c) -> handler.accept(f));
    }

    public abstract <M> BatchMessageHandler<E, R> buildWithMessageHandler(BiConsumer<M, Context> handler, Class<M> messageClass);

    public <M> BatchMessageHandler<E, R> buildWithMessageHandler(Consumer<M> handler, Class<M> messageClass) {
        return buildWithMessageHandler((f, c) -> handler.accept(f), messageClass);
    }


    protected abstract C getThis();
}
