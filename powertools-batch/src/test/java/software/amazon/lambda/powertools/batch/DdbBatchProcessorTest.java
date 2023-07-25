package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class DdbBatchProcessorTest {

    @Mock
    private Context context;

    private void processMessageSucceeds(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        // Great success
    }

    private void processMessageFailsForFixedMessage(DynamodbEvent.DynamodbStreamRecord record, Context context) {
        if (record.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withMessage(DynamodbEvent event) {
        // Arrange
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse.getBatchItemFailures()).hasSize(1);
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

    @ParameterizedTest
    @Event(value = "dynamo_event.json", type = DynamodbEvent.class)
    public void failingSuccessHandlerShouldntFailBatchButShouldFailMessage(DynamodbEvent event) {
        // Arrange
        AtomicBoolean wasCalledAndFailed = new AtomicBoolean(false);
        BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
                .withDynamoDbBatchHandler()
                .withSuccessHandler((e) -> {
                    if (e.getDynamodb().getSequenceNumber().equals("4421584500000000017450439091")) {
                        wasCalledAndFailed.set(true);
                        throw new RuntimeException("Success handler throws");
                    }
                })
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        StreamsEventResponse dynamodbBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(dynamodbBatchResponse).isNotNull();
        assertThat(dynamodbBatchResponse.getBatchItemFailures().size()).isEqualTo(1);
        assertThat(wasCalledAndFailed.get()).isTrue();
        StreamsEventResponse.BatchItemFailure batchItemFailure = dynamodbBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("4421584500000000017450439091");
    }

}
