package software.amazon.lambda.powertools.sqs.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.sqs.SqsBatchProcessor;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static software.amazon.lambda.powertools.sqs.internal.SqsMessageBatchProcessorAspectTest.mockedRandom;

public class PartialBatchPartialFailureHandler implements RequestHandler<SQSEvent, String> {
    @Override
    @SqsBatchProcessor(InnerMessageHandler.class)
    public String handleRequest(final SQSEvent sqsEvent,
                                final Context context) {
        return "Success";
    }

    private class InnerMessageHandler implements SqsMessageHandler<Object> {

        @Override
        public String process(SQSMessage message) {
            if ("2e1424d4-f796-459a-8184-9c92662be6da".equals(message.getMessageId())) {
                throw new RuntimeException("2e1424d4-f796-459a-8184-9c92662be6da");
            }

            mockedRandom.nextInt();
            return "Success";
        }
    }
}
