package software.amazon.lambda.powertools.batch;

import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.batch.SqsBatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.model.Basket;
import com.amazonaws.services.lambda.runtime.Context;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleBuilderTest {

    @Test
    public void sqsBuilderWorks() {
        SqsBatchMessageHandlerBuilder builder = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .withFailureHandler((c, e) -> System.out.println("Whoops!"))
                .withMessageHandler(this::processMessage);

        assertThat(builder).isNotNull();
    }

    public void processMessage(Basket basket, Context context) {

    }
}
