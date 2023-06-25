package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public abstract class KinesisBatchRequestHandler extends BatchRequestHandler<KinesisEvent, KinesisEvent.KinesisEventRecord, StreamsEventResponse> {

    @Override
    protected List<KinesisEvent.KinesisEventRecord> extractMessages(KinesisEvent input) {
        return input.getRecords();
    }

    @Override
    protected StreamsEventResponse writeResponse(Iterable<MessageProcessingResult<KinesisEvent.KinesisEventRecord>> results) {
        // Here we map up the kinesis-specific response for the batch based on the success of the individual messages
        throw new NotImplementedException();
    }

}
