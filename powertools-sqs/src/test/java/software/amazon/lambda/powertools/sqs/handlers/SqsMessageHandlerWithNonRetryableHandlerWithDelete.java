package software.amazon.lambda.powertools.sqs.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.sqs.SqsBatch;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static software.amazon.lambda.powertools.sqs.internal.SqsMessageBatchProcessorAspectTest.mockedRandom;

public class SqsMessageHandlerWithNonRetryableHandlerWithDelete implements RequestHandler<SQSEvent, String> {

    @Override
    @SqsBatch(value = InnerMessageHandler.class,
            nonRetryableExceptions = {IllegalStateException.class, IllegalArgumentException.class},
            deleteNonRetryableMessageFromQueue = true)
    public String handleRequest(final SQSEvent sqsEvent,
                                final Context context) {
        return "Success";
    }

    private class InnerMessageHandler implements SqsMessageHandler<Object> {

        @Override
        public String process(SQSMessage message) {
            if(message.getMessageId().isEmpty()) {
                throw new IllegalArgumentException("Invalid message and was moved to DLQ");
            }

            if("2e1424d4-f796-459a-9696-9c92662ba5da".equals(message.getMessageId())) {
                throw new RuntimeException("Invalid message and should be reprocessed");
            }

            mockedRandom.nextInt();
            return "Success";
        }
    }
}
