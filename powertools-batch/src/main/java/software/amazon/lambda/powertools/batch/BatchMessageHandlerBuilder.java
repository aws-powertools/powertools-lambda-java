package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.BatchMessageProcessor;
import software.amazon.lambda.powertools.batch.message.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Let's make this builder-style so that we can extend the interface
 * if we need to with extra fields without breaking clients
 */
public class BatchMessageHandlerBuilder {

    private Duration timeout;

    public BatchMessageHandlerBuilder() {
    }

    /**
     * An example parameter we might want to configure here to show
     * the shape of the interface.
     */
    public BatchMessageHandlerBuilder withTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    // TODO - can we put a meaningful type on the message, or do we need to do it, per-message-type?
    public List<String> process(SQSEvent message, Consumer<SqsMessage> messageHandler) {
        throw new NotImplementedException();
    };

    public List<String> process(KinesisEvent message, Consumer<KinesisDataStreamsMessage> messageHandler) {
        throw new NotImplementedException();
    }

    public List<String> process(DynamodbEvent message, Consumer<DynamoDbStreamMessage> messageHandler) {
        throw new NotImplementedException();
    }

}
