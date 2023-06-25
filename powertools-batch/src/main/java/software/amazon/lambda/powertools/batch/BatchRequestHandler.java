package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.xml.internal.ws.api.message.Message;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BatchRequestHandler<T, U, V> implements RequestHandler<T, V> {

    protected class MessageProcessingResult<U> {
        private final U message;
        private final Exception exception;

        public MessageProcessingResult(U message, Exception exception) {
            this.message = message;
            this.exception = exception;
        }
    }

    @Override
    public V handleRequest(T input, Context context) {
        // Extract messages
        List<U> messages = extractMessages(input);

        // Try process them
        List<MessageProcessingResult<U>> results = messages.stream().map(m -> {
            try {
                processItem(m, context);
                return new MessageProcessingResult<>(m, null);
            } catch (Exception e) {
                return new MessageProcessingResult<>(m, e);
            }
        }).collect(Collectors.toList());

        // Generate the response
        return writeResponse(results);
    }

    /**
     * Provided by the event-specific child to extract the individual records
     * from the batch request
     *
     * @param input
     * @return
     */
    protected abstract List<U> extractMessages(T input);

    protected abstract V writeResponse(Iterable<MessageProcessingResult<U>> results);

    public abstract void processItem(U message, Context context);
}
