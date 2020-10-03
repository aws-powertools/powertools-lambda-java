package software.amazon.lambda.powertools.sqs;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

/**
 * <p>
 * This interface should be implemented for processing {@link SQSMessage} inside {@link SQSEvent} received by lambda
 * function.
 * </p>
 *
 * <p>
 * It is required by utilities:
 * <ul>
 *   <li>{@link SqsBatchProcessor}</li>
 *   <li>{@link PowertoolsSqs#batchProcessor(SQSEvent, Class)}</li>
 *   <li>{@link PowertoolsSqs#batchProcessor(SQSEvent, boolean, Class)}</li>
 *   <li>{@link PowertoolsSqs#batchProcessor(SQSEvent, SqsMessageHandler)}</li>
 *   <li>{@link PowertoolsSqs#batchProcessor(SQSEvent, boolean, SqsMessageHandler)}</li>
 * </ul>
 * </p>
 * @param <R> Return value type from {@link SqsMessageHandler#process(SQSMessage)}
 */
@FunctionalInterface
public interface SqsMessageHandler<R> {

    R process(SQSMessage message);
}
