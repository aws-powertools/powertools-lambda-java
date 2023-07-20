package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.batch.SqsBatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
import software.amazon.lambda.powertools.model.Basket;
import com.amazonaws.services.lambda.runtime.Context;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleBuilderTest {

    @Test
    public void sqsBuilderWorks() {
        BatchMessageHandler<SQSEvent, SQSBatchResponse> messageHandler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .withFailureHandler((c, e) -> System.out.println("Whoops!"))
                .buildWithMessageHandler(this::processMessage);

        assertThat(messageHandler).isNotNull();
    }

    public void processMessage(Basket basket, Context context) {

    }
}
