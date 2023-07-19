package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;

import java.util.function.Consumer;

public class DdbMessageHandlerBuilder {

    public StreamsEventResponse processBatch(DynamodbEvent batch, Consumer<DynamodbEvent.DynamodbStreamRecord> handler) {
        throw new RuntimeException("Not implemented");
    }
}
