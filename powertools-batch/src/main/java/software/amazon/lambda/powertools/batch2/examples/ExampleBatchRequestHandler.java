package software.amazon.lambda.powertools.batch2.examples;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch2.ProcessSQSMessageBatch;

public class ExampleBatchRequestHandler extends ProcessSQSMessageBatch {

    // this method comes from the BatchProcessor interface, developers need to override the appropriate one
    @Override
    public void processItem(SQSEvent.SQSMessage message) {
        // do some stuff with this item
    }
}
