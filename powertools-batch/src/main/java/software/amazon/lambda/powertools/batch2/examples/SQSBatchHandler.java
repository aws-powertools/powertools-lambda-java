package software.amazon.lambda.powertools.batch2.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch2.SQSBatchProcessor;
import software.amazon.lambda.powertools.model.Basket;

public class SQSBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse>, SQSBatchProcessor<Basket> {
    @Override
    public SQSBatchResponse handleRequest(SQSEvent input, Context context) {
        return processBatch(input, context);
    }

    @Override
    public void processItem(Basket basket, Context context) {
        // do some stuff with the item
        System.out.println(basket.toString());
    }
}
