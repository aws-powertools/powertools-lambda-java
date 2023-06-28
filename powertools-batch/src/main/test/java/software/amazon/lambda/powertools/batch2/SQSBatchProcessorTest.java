package software.amazon.lambda.powertools.batch2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
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

    static class SQSBP implements SQSBatchProcessor<SQSEvent.SQSMessage> {
        public void processItem(SQSEvent.SQSMessage message, Context context) {
            System.out.println(message.toString());
        }
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testBasic(SQSEvent event) {
        SQSBP sqsbp = new SQSBP();
        sqsbp.processBatch(event, context);
    }

    @ParameterizedTest
    @Event(value = "sqs_event.json", type = SQSEvent.class)
    public void testCustom(SQSEvent event) {
        SQSBProduct sqsbproduct = new SQSBProduct();
        sqsbproduct.processBatch(event, context);
    }
}
