package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import java.util.Optional;

public class LargeMessageProcessorFactory {

    private LargeMessageProcessorFactory() {
        // not intended to be instantiated
    }

    public static Optional<LargeMessageProcessor<?>> get(Object message) {
        if (message instanceof SQSMessage) {
            return Optional.of(new LargeSQSMessageProcessor());
        } else if (message instanceof SNSRecord) {
            return Optional.of(new LargeSNSMessageProcessor());
        } else {
            return Optional.empty();
        }
    }
}
