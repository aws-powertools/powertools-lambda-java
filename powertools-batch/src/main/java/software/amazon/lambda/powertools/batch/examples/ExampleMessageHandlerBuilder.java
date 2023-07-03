package software.amazon.lambda.powertools.batch.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;

/**
 * An example handler that is implemented by using the {@link BatchMessageHandlerBuilder} inline
 * in an existing RequestHandler.
 */
public class ExampleMessageHandlerBuilder implements RequestHandler<SQSEvent, Object> {

    @Override
    public Object handleRequest(SQSEvent sqsEvent, Context context) {
        return new BatchMessageHandlerBuilder()
                .process(sqsEvent, (SQSEvent.SQSMessage message) ->  {
                    // Process the message without throwing an exception
                });
    }

}
