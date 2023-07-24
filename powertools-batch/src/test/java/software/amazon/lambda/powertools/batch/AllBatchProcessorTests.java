package software.amazon.lambda.powertools.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.amazonaws.services.lambda.runtime.tests.annotations.Events;
import org.junit.jupiter.params.ParameterizedTest;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test functionality that should be common to all the batch processors
 *
 * TODO probably remove. Trying to think of a way of factoring
 * TODO common tests for all the message handlers into one place
 */
public class AllBatchProcessorTests {
//
//    AtomicBoolean failureHandlerWasCalled = new AtomicBoolean(false);
//    public <T> void failingFailureHandler(T evt, Throwable t) {
//        failureHandlerWasCalled.set(true);
//        throw new RuntimeException("Well, this doesn't look great");
//    }
//
//    @ParameterizedTest
//    @Events(
//            events = {
//                    @Event(value = "sqs_event.json", type = SQSEvent.class),
//                    @Event(value = "kinesis_event.json", type = KinesisEvent.class)
//            }
//    )
//    public void failingFailureHandlerShouldntFailBatch(Object event) {
//        BatchMessageHandler handler;
//        if (event instanceof SQSEvent) {
//            handler = new BatchMessageHandlerBuilder()
//                    .withSqsBatchHandler()
//                    .withFailureHandler(this::failingFailureHandler)
//                    .build
//        }
//    }

}
