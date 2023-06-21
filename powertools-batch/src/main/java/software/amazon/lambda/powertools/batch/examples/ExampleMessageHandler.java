package software.amazon.lambda.powertools.batch.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.BatchMessageProcessor;
import software.amazon.lambda.powertools.batch.message.BatchProcessorMessageType;
import software.amazon.lambda.powertools.batch.message.SqsMessage;

import java.util.List;

/**
 * This is just here for illustrative purposes, and won't
 * be released with this code!
 */
public class ExampleMessageHandler implements RequestHandler<SQSEvent, List<String>> {

    @Override
    public List<String> handleRequest(SQSEvent sqsEvent, Context context) {
        return new BatchMessageHandlerBuilder()
                .process(sqsEvent, (SqsMessage message) ->  {
                    // Process the message without throwing an exception
                });
    }

}
