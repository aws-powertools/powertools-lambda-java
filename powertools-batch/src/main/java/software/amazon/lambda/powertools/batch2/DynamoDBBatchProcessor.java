package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public interface DynamoDBBatchProcessor extends BatchProcessor<DynamodbEvent, Void, StreamsEventResponse> {

    Logger DDB_BATCH_LOGGER = LoggerFactory.getLogger(SQSBatchProcessor.class);

    @Override
    default StreamsEventResponse processBatch(DynamodbEvent event, Context context) {
        StreamsEventResponse response = StreamsEventResponse.builder().withBatchItemFailures(new ArrayList<>()).build();
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            try {
                processRecord(record, context);
            } catch (Throwable t) {
                DDB_BATCH_LOGGER.error("Error while processing record with id {}: {}, adding it to batch item failures", record.getEventID(), t.getMessage());
                response.getBatchItemFailures().add(BatchItemFailure.builder().withItemIdentifier(record.getEventID()).build());
            }
        }
        return response;
    }

    @Override
    default void processItem(Void message, Context context) {
        DDB_BATCH_LOGGER.warn("[DEFAULT IMPLEMENTATION] This method should not be used in the case of DynamoDB");
    }

    void processRecord(DynamodbEvent.DynamodbStreamRecord record, Context context);

}
