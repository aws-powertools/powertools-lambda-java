package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

public interface DynamoDBBatchProcessor extends BatchProcessor<DynamodbEvent, Void, StreamsEventResponse> {

    @Override
    default StreamsEventResponse processBatch(DynamodbEvent event, Context context) {
        StreamsEventResponse response = new StreamsEventResponse();
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            try {
                processItem(record, context);
            } catch (Throwable t) {
                response.getBatchItemFailures().add(new StreamsEventResponse.BatchItemFailure(record.getEventID()));
            }
        }
        return response;
    }

    @Override
    default void processItem(Void message, Context context) {
        System.out.println("Nothing to do here");
    }

    void processItem(DynamodbEvent.DynamodbStreamRecord record, Context context);

}
