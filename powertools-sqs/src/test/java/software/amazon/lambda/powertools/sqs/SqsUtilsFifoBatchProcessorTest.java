package software.amazon.lambda.powertools.sqs;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.sqs.SqsUtils.batchProcessor;
import static software.amazon.lambda.powertools.sqs.SqsUtils.overrideSqsClient;

public class SqsUtilsFifoBatchProcessorTest {

    private static SQSEvent sqsBatchEvent;
    private MockitoSession session;

    @Mock
    private SqsClient sqsClient;

    @Captor
    private ArgumentCaptor<DeleteMessageBatchRequest> deleteMessageBatchCaptor;

    public SqsUtilsFifoBatchProcessorTest() throws IOException {
        sqsBatchEvent = EventLoader.loadSQSEvent("SqsFifoBatchEvent.json");
    }

    @BeforeEach
    public void setup() {
        // Establish a strict mocking session. This means that any
        // calls to a mock that have not been stubbed will throw
        this.session = Mockito.mockitoSession()
                .strictness(Strictness.STRICT_STUBS)
                .initMocks(this)
                .startMocking();

        overrideSqsClient(sqsClient);
    }

    @AfterEach
    public void tearDown() {
        session.finishMocking();
    }

    @Test
    public void processWholeBatch() {
        // Act
        AtomicInteger processedCount = new AtomicInteger();
        List<Object> results = batchProcessor(sqsBatchEvent, false, (message) -> {
            processedCount.getAndIncrement();
            return true;
        });

        // Assert
        assertThat(processedCount.get()).isEqualTo(3);
        assertThat(results.size()).isEqualTo(3);
    }

    @Test
    public void singleFailureAtEndOfBatch() {

        // Arrange
        Mockito.when(sqsClient.deleteMessageBatch(deleteMessageBatchCaptor.capture())).thenReturn(DeleteMessageBatchResponse
                .builder().build());


        // Act
        AtomicInteger processedCount = new AtomicInteger();
        assertThatExceptionOfType(SQSBatchProcessingException.class)
                .isThrownBy(() -> batchProcessor(sqsBatchEvent, false, (message) -> {
                    int value = processedCount.getAndIncrement();
                    if (value == 2) {
                        throw new RuntimeException("Whoops");
                    }
                    return true;
        }));

        // Assert
        DeleteMessageBatchRequest deleteRequest = deleteMessageBatchCaptor.getValue();
        List<String> messageIds = deleteRequest.entries().stream()
                .map(DeleteMessageBatchRequestEntry::id)
                .collect(Collectors.toList());
        assertThat(deleteRequest.entries().size()).isEqualTo(2);
        assertThat(messageIds.contains(sqsBatchEvent.getRecords().get(0).getMessageId())).isTrue();
        assertThat(messageIds.contains(sqsBatchEvent.getRecords().get(1).getMessageId())).isTrue();
        assertThat(messageIds.contains(sqsBatchEvent.getRecords().get(2).getMessageId())).isFalse();
    }

    @Test
    public void messageFailureStopsGroupProcessing() {
        String groupToFail = sqsBatchEvent.getRecords().get(0).getAttributes().get("MessageGroupId");

        assertThatExceptionOfType(SQSBatchProcessingException.class)
                .isThrownBy(() -> batchProcessor(sqsBatchEvent, (message) -> {
                    String groupId = message.getAttributes().get("MessageGroupId");
                    if (groupId.equals(groupToFail)) {
                        throw new RuntimeException("Failed processing");
                    }
                    return groupId;
                }))
            .satisfies(e -> {
                assertThat(e.successMessageReturnValues().size()).isEqualTo(0);
                assertThat(e.successMessageReturnValues().contains(groupToFail)).isFalse();
            });
    }

}
