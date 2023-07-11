package software.amazon.lambda.powertools.batch3;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.function.Consumer;

public class DdbMessageHandlerBuilder {

    public StreamsEventResponse processBatch(DynamodbEvent batch, Consumer<DynamodbEvent.DynamodbStreamRecord> handler) {
        throw new NotImplementedException();
    }
}
