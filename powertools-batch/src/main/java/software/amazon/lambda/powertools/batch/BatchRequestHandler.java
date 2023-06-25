package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.xml.internal.ws.api.message.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An abstract base class that will be extended per-event-source to supply event-source
 * specific functionality. As much as possible is pulled into the base class, and
 * event-source specific logic is delegated downwards (e.g., mapping errors to response,
 * extracting messages from batch).
 *
 * IRL, this would be implemented leaning on the {@link BatchMessageHandlerBuilder}, here
 * I have provided implementation inline for illustrative purposes.
 *
 * @param <T> The batch event type
 * @param <U> The individual message type for each message within teh batch
 * @param <V> The batch result type
 */
public abstract class BatchRequestHandler<T, U, V> implements RequestHandler<T, V> {

    /**
     * Used to wrap the result of processing a single message. We wrap the message itself,
     * and optionally an exception that was raised during the processing. A lack of
     * exception indicates success.
     */
    protected class MessageProcessingResult<U> {
        private final U message;
        private final Exception exception;

        public MessageProcessingResult(U message, Exception exception) {
            this.message = message;
            this.exception = exception;
        }
    }

    /**
     * The batch processing logic goes here. This can be generic across all message types.
     *
     * @param input The batch message to process
     * @param context The Lambda execution environment context object.
     * @return
     */
    @Override
    public V handleRequest(T input, Context context) {
        // Extract messages from batch
        List<U> messages = extractMessages(input);

        // For each message, map it to either 1/ a successful result, or 2/ an exception
        List<MessageProcessingResult<U>> results = messages.stream().map(m -> {
            try {
                enhanceMessage(m);
                processItem(m, context);
                return new MessageProcessingResult<>(m, null);
            } catch (Exception e) {
                return new MessageProcessingResult<>(m, e);
            }
        }).collect(Collectors.toList());

        // Generate the response from the list of results
        return writeResponse(results);
    }

    /**
     * Provided by the event-specific child to extract the individual records
     * from the batch request
     *
     * @param input The batch
     * @return the messages within the batch
     */
    protected abstract List<U> extractMessages(T input);

    /**
     * Given the set of message processing results, generates the appropriate batch
     * processing result to return to Lambda.
     *
     * @param results the result of processing each message, and the messages themselves
     * @return the batch processing result to return to lambda
     */
    protected abstract V writeResponse(Iterable<MessageProcessingResult<U>> results);

    /**
     * To be provided by the user. Processes an individual message within the batch
     * @param message
     * @param context
     */
    public abstract void processItem(U message, Context context);

    /**
     * This could be overriden by event-specific children to implement things like large
     * message processing.
     * @param message
     */
    protected void enhanceMessage(U message) {

    }
}
