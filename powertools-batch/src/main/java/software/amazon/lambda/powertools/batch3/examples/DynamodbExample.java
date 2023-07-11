package software.amazon.lambda.powertools.batch3.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch3.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.model.Basket;

public class DynamodbExample implements RequestHandler<DynamodbEvent, Object> {

    @Override
    public Object handleRequest(DynamodbEvent ddbEvent, Context context) {
        // Example 2 - process a deserialized message
        return new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .processBatch(ddbEvent, basket -> {

                });
    }
}