package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;

/**
 *
 * @param <E> The type of the Lambda event
 * @param <R> The type of the lambda response
 */
public interface BatchMessageHandler<E, R> {

    public abstract R processBatch(E event, Context context);

}
