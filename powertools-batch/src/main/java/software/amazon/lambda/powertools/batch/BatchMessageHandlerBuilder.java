package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.events.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;


/**
 * A builder-style interface we can use within an existing Lambda RequestHandler to
 * deal with our batch responses.
 *
 * @see software.amazon.lambda.powertools.batch.examples.ExampleMessageHandlerBuilder
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
    public SQSBatchResponse process(SQSEvent message, Consumer<SQSEvent.SQSMessage> messageHandler) {
        throw new NotImplementedException();
    };

    public StreamsEventResponse process(KinesisEvent message, Consumer<KinesisEvent.KinesisEventRecord> messageHandler) {
        throw new NotImplementedException();
    }

    public StreamsEventResponse process(DynamodbEvent message, Consumer<DynamodbEvent.DynamodbStreamRecord> messageHandler) {
        throw new NotImplementedException();
    }

}
