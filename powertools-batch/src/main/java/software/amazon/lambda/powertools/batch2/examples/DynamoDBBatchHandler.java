package software.amazon.lambda.powertools.batch2.examples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import software.amazon.lambda.powertools.batch2.DynamoDBBatchProcessor;

public class DynamoDBBatchHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse>, DynamoDBBatchProcessor {
    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent input, Context context) {
        return processBatch(input, context);
    }

    @Override
    public void processItem(DynamodbEvent.DynamodbStreamRecord record, Context context) {

    }
}
