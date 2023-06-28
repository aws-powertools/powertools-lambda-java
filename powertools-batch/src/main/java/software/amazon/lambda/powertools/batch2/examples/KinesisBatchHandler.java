package software.amazon.lambda.powertools.batch2.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import software.amazon.lambda.powertools.batch2.KinesisBatchProcessor;
import software.amazon.lambda.powertools.batch2.examples.model.Basket;

public class KinesisBatchHandler implements RequestHandler<KinesisEvent, StreamsEventResponse>, KinesisBatchProcessor<Basket> {
    @Override
    public StreamsEventResponse handleRequest(KinesisEvent input, Context context) {
        return processBatch(input, context);
    }

    @Override
    public void processItem(Basket basket, Context context) {
        // do some stuff with the item
        System.out.println(basket.toString());
    }
}
