package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.batch.model.Product;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SQSBatchProcessorTest {
    @Mock
    private Context context;

    // A handler that works
    private void processMessageSucceeds(SQSEvent.SQSMessage sqsMessage) {
    }

    // A handler that throws an exception for _one_ of the sample messages
    private void processMessageFailsForFixedMessage(SQSEvent.SQSMessage message, Context context) {
        if (message.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
            throw new RuntimeException("fake exception");
        }
    }

    // A handler that throws an exception for _one_ of the deserialized products in the same messages
    public void processMessageFailsForFixedProduct(Product product, Context context) {
        if (product.getId() == 12345) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withMessage(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_fifo_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withSQSFIFO(SQSEvent event) {
        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailsForFixedMessage);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(2);
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
        batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(1);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("f9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }


    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withProduct(SQSEvent event) {

        // Arrange
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);
        assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);

        // Assert
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void failingFailureHandlerShouldntFailBatch(SQSEvent event) {
        // Arrange
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .withFailureHandler((e, ex) -> {
                    wasCalled.set(true);
                    throw new RuntimeException("Well, this doesn't look great");
                })
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse).isNotNull();
        assertThat(wasCalled.get()).isTrue();
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void failingSuccessHandlerShouldntFailBatchButShouldFailMessage(SQSEvent event) {
        // Arrange
        AtomicBoolean wasCalledAndFailed = new AtomicBoolean(false);
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .withSuccessHandler((e) -> {
                    if (e.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
                        wasCalledAndFailed.set(true);
                        throw new RuntimeException("Success handler throws");
                    }
                })
                .buildWithRawMessageHandler(this::processMessageSucceeds);

        // Act
        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        // Assert
        assertThat(sqsBatchResponse).isNotNull();
        assertThat(wasCalledAndFailed.get()).isTrue();
        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }



}
