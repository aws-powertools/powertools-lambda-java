package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.model.Product;

public class SQSBatchProcessorTest {
    @Mock
    private Context context;


    static class SQSBP implements SQSBatchProcessor<SQSEvent.SQSMessage> {
        public void processMessage(SQSEvent.SQSMessage message, Context context) {
            if (message.getMessageId().equals("e9144555-9a4f-4ec3-99a0-34ce359b4b54")) {
                throw new RuntimeException("fake exception");
            }
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withMessage(SQSEvent event) {
        SQSBP sqsbp = new SQSBP();
        SQSBatchResponse sqsBatchResponse = sqsbp.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);

        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    @ParameterizedTest
    @Event(value = "sqs_fifo_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withSQSFIFO(SQSEvent event) {
        SQSBP sqsbp = new SQSBP();
        SQSBatchResponse sqsBatchResponse = sqsbp.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(2);

        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
        batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(1);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("f9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

    static class SQSBProduct implements SQSBatchProcessor<Product> {
        public void processItem(Product product, Context context) {
            if (product.getId() == 12345) {
                throw new RuntimeException("fake exception");
            }
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void shouldAddMessageToBatchFailure_whenException_withProduct(SQSEvent event) {
        SQSBProduct sqsbproduct = new SQSBProduct();
        SQSBatchResponse sqsBatchResponse = sqsbproduct.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).hasSize(1);

        SQSBatchResponse.BatchItemFailure batchItemFailure = sqsBatchResponse.getBatchItemFailures().get(0);
        Assertions.assertThat(batchItemFailure.getItemIdentifier()).isEqualTo("e9144555-9a4f-4ec3-99a0-34ce359b4b54");
    }

}
