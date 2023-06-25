package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public abstract class SQSBatchRequestHandler extends BatchRequestHandler<SQSEvent, SQSEvent.SQSMessage, SQSBatchResponse> {

    @Override
    protected List<SQSEvent.SQSMessage> extractMessages(SQSEvent input) {
        return input.getRecords();
    }

    @Override
    protected SQSBatchResponse writeResponse(Iterable<MessageProcessingResult<SQSEvent.SQSMessage>> results) {
        // Here we map up the SQS-specific response for the batch based on the success of the individual messages
        throw new NotImplementedException();
    }
}
