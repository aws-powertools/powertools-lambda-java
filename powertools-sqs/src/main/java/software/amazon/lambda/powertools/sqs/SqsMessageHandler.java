package software.amazon.lambda.powertools.sqs;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

/**
 *
 * @param <R>
 */
@FunctionalInterface
public interface SqsMessageHandler<R> {

    R process(SQSMessage message);
}
