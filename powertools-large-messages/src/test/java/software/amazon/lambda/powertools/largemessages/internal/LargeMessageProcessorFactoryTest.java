package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LargeMessageProcessorFactoryTest {

    @Test
    public void createLargeSQSMessageProcessor() {
        assertThat(LargeMessageProcessorFactory.get(new SQSEvent.SQSMessage()))
                .isPresent()
                .get()
                .isInstanceOf(LargeSQSMessageProcessor.class);
    }

    @Test
    public void createLargeSNSMessageProcessor() {
        assertThat(LargeMessageProcessorFactory.get(new SNSEvent.SNSRecord()))
                .isPresent()
                .get()
                .isInstanceOf(LargeSNSMessageProcessor.class);
    }

    @Test
    public void createUnknownMessageProcessor() {
        assertThat(LargeMessageProcessorFactory.get(new KinesisEvent.KinesisEventRecord())).isNotPresent();
    }
}
