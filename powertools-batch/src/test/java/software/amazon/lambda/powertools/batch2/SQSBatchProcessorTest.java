package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import software.amazon.lambda.powertools.batch2.examples.model.Product;

public class SQSBatchProcessorTest {
    @Mock
    private Context context;
    static class SQSBProduct implements SQSBatchProcessor<Product> {
        public void processItem(Product product, Context context) {
            System.out.println(product);
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testCustom(SQSEvent event) {
        SQSBProduct sqsbproduct = new SQSBProduct();
        SQSBatchResponse sqsBatchResponse = sqsbproduct.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).isEmpty();
    }

    static class SQSBP implements SQSBatchProcessor<SQSEvent.SQSMessage> {
        public void processMessage(SQSEvent.SQSMessage message, Context context) {
            System.out.println(message.toString());
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testBasic(SQSEvent event) {
        SQSBP sqsbp = new SQSBP();
        SQSBatchResponse sqsBatchResponse = sqsbp.processBatch(event, context);
        Assertions.assertThat(sqsBatchResponse.getBatchItemFailures()).isEmpty();
    }
}
