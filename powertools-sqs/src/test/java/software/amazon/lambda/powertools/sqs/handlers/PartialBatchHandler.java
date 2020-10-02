package software.amazon.lambda.powertools.sqs.handlers;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.sqs.LargeMessageHandler;
import software.amazon.lambda.powertools.sqs.PowertoolsSqs;
import software.amazon.lambda.powertools.sqs.SqsBatchProcessor;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;

public class PartialBatchHandler implements RequestHandler<SQSEvent, List<Object>> {


    @Override
    @LargeMessageHandler
    @SqsBatchProcessor(HandlerSqs.class)
    public List<Object> handleRequest(SQSEvent sqsEvent, Context context) {

        List<Object> returnValues =
                PowertoolsSqs.partialBatchProcessor(sqsEvent, false, HandlerSqs.class);

        // Do some processing on processed message

        return returnValues;
    }

    private class HandlerSqs implements SqsMessageHandler<Object> {

        @Override
        public String process(SQSEvent.SQSMessage message) {
            // This is where you process message
            return null;
        }
    }
}
