package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.model.Product;

public class SQSBatchProcessorTest {
    @Mock
    private Context context;


    private void processMessageFailSometimes(SQSEvent.SQSMessage message, Context context) {
        if (message.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
            throw new RuntimeException("fake exception");
        }
    }

    public void processMessageFailsForFixedProduct(Product product, Context context) {
        if (product.getId() == 12345) {
            throw new RuntimeException("fake exception");
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withMessage(SQSEvent event) {
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailSometimes);

        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);

        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_fifo_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withSQSFIFO(SQSEvent event) {
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processMessageFailSometimes);

        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);

        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(2);

        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
        batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(1);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("f9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }


    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withProduct(SQSEvent event) {

        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessageFailsForFixedProduct, Product.class);

        SQSBatchResponse sqsBatchResponse = handler.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);

        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

}
