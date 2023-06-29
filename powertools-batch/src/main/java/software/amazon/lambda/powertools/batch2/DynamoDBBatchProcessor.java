package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

import java.util.ArrayList;

public interface DynamoDBBatchProcessor extends BatchProcessor<DynamodbEvent, Void, StreamsEventResponse> {

    @Override
    default StreamsEventResponse processBatch(DynamodbEvent event, Context context) {
        StreamsEventResponse response = StreamsEventResponse.builder().withBatchItemFailures(new ArrayList<>()).build();
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            try {
                processRecord(record, context);
            } catch (Throwable t) {
                response.getBatchItemFailures().add(new StreamsEventResponse.BatchItemFailure(record.getEventID()));
            }
        }
        return response;
    }

    @Override
    default void processItem(Void message, Context context) {
        System.out.println("This method is not used in the case of DynamoDB");
    }

    void processRecord(DynamodbEvent.DynamodbStreamRecord record, Context context);

}
