package software.amazon.lambda.powertools.sqs;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.sqs.SqsClient;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PowertoolsSqsBatchProcessorTest {

    private final SqsClient sqsClient = mock(SqsClient.class);
    private final SqsClient interactionClient = mock(SqsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SQSEvent event;

    @BeforeEach
    void setUp() throws IOException {
        event = MAPPER.readValue(this.getClass().getResource("/sampleSqsBatchEvent.json"), SQSEvent.class);
        PowertoolsSqs.defaultSqsClient(sqsClient);
    }

    @Test
    void shouldBatchProcessAndNotDeleteMessagesWhenAllSuccess() {
        List<String> returnValues = PowertoolsSqs.partialBatchProcessor(event, false, (message) -> {
            interactionClient.listQueues();
            return "Success";
        });

        assertThat(returnValues)
                .hasSize(2)
                .containsExactly("Success", "Success");

        verify(interactionClient, times(2)).listQueues();
        verifyNoInteractions(sqsClient);
    }

    @ParameterizedTest
    @ValueSource(classes = {SampleInnerSqsHandler.class, SampleSqsHandler.class})
    void shouldBatchProcessViaClassAndNotDeleteMessagesWhenAllSuccess(Class<? extends SqsMessageHandler<String>> handler) {
        List<String> returnValues = PowertoolsSqs.partialBatchProcessor(event, false, handler);

        assertThat(returnValues)
                .hasSize(2)
                .containsExactly("0", "1");
    }

    private static Stream<Arguments> exception() {
        return Stream.of(Arguments.of(new AmazonServiceException("Service Exception")),
                Arguments.of(new SdkClientException("Client Exception")));
    }

    public class SampleInnerSqsHandler implements SqsMessageHandler<String> {
        private int counter;

        @Override
        public String process(SQSMessage message) {
            return String.valueOf(counter++);
        }
    }
}