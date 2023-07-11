package software.amazon.lambda.powertools.batch3.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch3.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.model.Basket;

/**
 * An example handler that is implemented by using the {@link BatchMessageHandlerBuilder} inline
 * in an existing RequestHandler.
 */
public class SqsExampleWithDeserialization implements RequestHandler<SQSEvent, Object> {

    @Override
    public Object handleRequest(SQSEvent sqsEvent, Context context) {
        // Example 2 - process a deserialized message
        return new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .<Basket>processMessage(sqsEvent, basket -> {

                });
    }
}

