package software.amazon.lambda.powertools.sqs.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.sqs.SqsBatch;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static software.amazon.lambda.powertools.sqs.internal.SqsMessageBatchProcessorAspectTest.mockedRandom;

public class PartialBatchSuccessHandler implements RequestHandler<SQSEvent, String> {
    @Override
    @SqsBatch(InnerMessageHandler.class)
    public String handleRequest(final SQSEvent sqsEvent,
                                final Context context) {
        return "Success";
    }

    private class InnerMessageHandler implements SqsMessageHandler<Object> {

        @Override
        public String process(SQSMessage message) {
            mockedRandom.nextInt();
            return "Success";
        }
    }
}
