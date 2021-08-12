package software.amazon.lambda.powertools.sqs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.lambda.powertools.sqs.SqsUtils.batchProcessor;
import static software.amazon.lambda.powertools.sqs.SqsUtils.overrideSqsClient;

class SqsUtilsBatchProcessorTest {

    private static final SqsClient sqsClient = mock(SqsClient.class);
    private static final SqsClient interactionClient = mock(SqsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SQSEvent event;

    @BeforeEach
    void setUp() throws IOException {
        reset(sqsClient, interactionClient);
        event = MAPPER.readValue(this.getClass().getResource("/sampleSqsBatchEvent.json"), SQSEvent.class);
        overrideSqsClient(sqsClient);
    }

    @Test
    void shouldBatchProcessAndNotDeleteMessagesWhenAllSuccess() {
        List<String> returnValues = batchProcessor(event, false, (message) -> {
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
        List<String> returnValues = batchProcessor(event, handler);

        assertThat(returnValues)
                .hasSize(2)
                .containsExactly("0", "1");

        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldBatchProcessAndDeleteSuccessMessageOnPartialFailures() {
        String failedId = "2e1424d4-f796-459a-8184-9c92662be6da";

        SqsMessageHandler<String> failedHandler = (message) -> {
            if (failedId.equals(message.getMessageId())) {
                throw new RuntimeException("Failed processing");
            }

            interactionClient.listQueues();
            return "Success";
        };

        assertThatExceptionOfType(SQSBatchProcessingException.class)
                .isThrownBy(() -> batchProcessor(event, failedHandler))
                .satisfies(e -> {

                    assertThat(e.successMessageReturnValues())
                            .hasSize(1)
                            .contains("Success");

                    assertThat(e.getFailures())
                            .hasSize(1)
                            .extracting("messageId")
                            .contains(failedId);

                    assertThat(e.getExceptions())
                            .hasSize(1)
                            .extracting("detailMessage")
                            .contains("Failed processing");
                });

        verify(interactionClient).listQueues();

        ArgumentCaptor<DeleteMessageBatchRequest> captor = ArgumentCaptor.forClass(DeleteMessageBatchRequest.class);
        verify(sqsClient).deleteMessageBatch(captor.capture());

        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("queueUrl", "https://sqs.us-east-2.amazonaws.com/123456789012/my-queue");
    }

    @Test
    void shouldBatchProcessAndFullFailuresInBatch() {
        SqsMessageHandler<String> failedHandler = (message) -> {
            throw new RuntimeException(message.getMessageId());
        };

        assertThatExceptionOfType(SQSBatchProcessingException.class)
                .isThrownBy(() -> batchProcessor(event, failedHandler))
                .satisfies(e -> {

                    assertThat(e.successMessageReturnValues())
                            .isEmpty();

                    assertThat(e.getFailures())
                            .hasSize(2)
                            .extracting("messageId")
                            .containsExactly("059f36b4-87a3-44ab-83d2-661975830a7d",
                                    "2e1424d4-f796-459a-8184-9c92662be6da");

                    assertThat(e.getExceptions())
                            .hasSize(2)
                            .extracting("detailMessage")
                            .containsExactly("059f36b4-87a3-44ab-83d2-661975830a7d",
                                    "2e1424d4-f796-459a-8184-9c92662be6da");
                });

        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldBatchProcessViaClassAndDeleteSuccessMessageOnPartialFailures() {
        assertThatExceptionOfType(SQSBatchProcessingException.class)
                .isThrownBy(() -> batchProcessor(event, FailureSampleInnerSqsHandler.class))
                .satisfies(e -> {

                    assertThat(e.successMessageReturnValues())
                            .hasSize(1)
                            .contains("Success");

                    assertThat(e.getFailures())
                            .hasSize(1)
                            .extracting("messageId")
                            .contains("2e1424d4-f796-459a-8184-9c92662be6da");

                    assertThat(e.getExceptions())
                            .hasSize(1)
                            .extracting("detailMessage")
                            .contains("Failed processing");
                });

        verify(sqsClient).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }


    @Test
    void shouldBatchProcessAndSuppressExceptions() {
        String failedId = "2e1424d4-f796-459a-8184-9c92662be6da";

        SqsMessageHandler<String> failedHandler = (message) -> {
            if (failedId.equals(message.getMessageId())) {
                throw new RuntimeException("Failed processing");
            }

            interactionClient.listQueues();
            return "Success";
        };

        List<String> returnValues = batchProcessor(event, true, failedHandler);

        assertThat(returnValues)
                .hasSize(1)
                .contains("Success");

        verify(interactionClient).listQueues();
        verify(sqsClient).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    @Test
    void shouldBatchProcessViaClassAndSuppressExceptions() {
        List<String> returnValues = batchProcessor(event, true, FailureSampleInnerSqsHandler.class);

        assertThat(returnValues)
                .hasSize(1)
                .contains("Success");

        verify(interactionClient).listQueues();
        verify(sqsClient).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    public class SampleInnerSqsHandler implements SqsMessageHandler<String> {
        private int counter;

        @Override
        public String process(SQSMessage message) {
            interactionClient.listQueues();
            return String.valueOf(counter++);
        }
    }

    @Test
    void shouldBatchProcessAndMoveNonRetryableExceptionToDlq() {
        String failedId = "2e1424d4-f796-459a-8184-9c92662be6da";
        HashMap<QueueAttributeName, String> attributes = new HashMap<>();

        attributes.put(QueueAttributeName.REDRIVE_POLICY, "{\n" +
                "  \"deadLetterTargetArn\": \"arn:aws:sqs:us-east-2:123456789012:retry-queue\",\n" +
                "  \"maxReceiveCount\": 2\n" +
                "}");

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(GetQueueAttributesResponse.builder()
                        .attributes(attributes)
                .build());

        List<String> batchProcessor = batchProcessor(event, (message) -> {
            if (failedId.equals(message.getMessageId())) {
                throw new IllegalStateException("Failed processing");
            }

            interactionClient.listQueues();
            return "Success";
        }, IllegalStateException.class, IllegalArgumentException.class);

        Assertions.assertThat(batchProcessor)
                .hasSize(1);

        verify(sqsClient).sendMessageBatch(any(Consumer.class));
    }

    public class FailureSampleInnerSqsHandler implements SqsMessageHandler<String> {
        @Override
        public String process(SQSEvent.SQSMessage message) {
            if ("2e1424d4-f796-459a-8184-9c92662be6da".equals(message.getMessageId())) {
                throw new RuntimeException("Failed processing");
            }

            interactionClient.listQueues();
            return "Success";
        }
    }
}