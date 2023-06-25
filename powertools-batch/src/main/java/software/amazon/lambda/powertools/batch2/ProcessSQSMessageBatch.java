package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public class ProcessSQSMessageBatch implements RequestHandler<SQSEvent, SQSBatchResponse>, BatchProcessor<SQSEvent, SQSBatchResponse> {

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        // processBatch is implemented as default in the interface and handle exceptions in the processElement function to add the item to the BatchItemFailure list

        // TODO - as we're getting this on the outside, we could pass in `sqsEvent.getMessages()` which would
        // fix the "how do I extract event-specific message" problem in the interface default.
        return this.processBatch(sqsEvent); // we may need to pass context too... ?

        // TODO - as we're still on the outside, we could take the list of results from `this.processBatch()` and
        // map it back to the SQS-specific response, again solving the "how do I do this event-specific thing"
        // problem in the base class.
    }

    // this method comes from the BatchProcessor interface, developers need to override the appropriate one
    @Override
    public void processItem(SQSEvent.SQSMessage message) {
        // do some stuff with this item
    }

}