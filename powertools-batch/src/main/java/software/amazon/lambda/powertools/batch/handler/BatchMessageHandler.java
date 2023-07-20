package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;

/**
 *
 * The basic interface a batch message handler must meet.
 *
 * @param <E> The type of the Lambda batch event
 * @param <R> The type of the lambda batch response
 */
public interface BatchMessageHandler<E, R> {

    /**
     * Processes the given batch returning a partial batch
     * response indicating the success and failure of individual
     * messages within the batch.
     *
     * @param event The Lambda event containing the batch to process
     * @param context The lambda context
     * @return A partial batch response
     */
    public abstract R processBatch(E event, Context context);

}
