package software.amazon.lambda.powertools.sqs;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.lambda.powertools.sqs.SqsUtils.batchProcessor;
import static software.amazon.lambda.powertools.sqs.SqsUtils.overrideSqsClient;

public class SqsUtilsFifoBatchProcessorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static SQSEvent sqsBatchEvent;

    @Mock
    private SqsClient sqsClient;

    public SqsUtilsFifoBatchProcessorTest() throws IOException {
        openMocks(this);
        overrideSqsClient(sqsClient);
        sqsBatchEvent  =  MAPPER.readValue(SqsUtilsFifoBatchProcessorTest.class.getResource("/SqsFifoBatchEvent.json"), SQSEvent.class);
    }

    @Test
    public void processWholeBatch() {
        AtomicInteger processedCount = new AtomicInteger();
        List<Object> results = batchProcessor(sqsBatchEvent, false, (message) -> {
            processedCount.getAndIncrement();
            return true;
        });

        assertThat(processedCount.get()).isEqualTo(3);
        assertThat(results.size()).isEqualTo(3);
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
                assertThat(e.successMessageReturnValues().size()).isEqualTo(1);
                assertThat(e.successMessageReturnValues().contains(groupToFail)).isFalse();
            });
    }

}
