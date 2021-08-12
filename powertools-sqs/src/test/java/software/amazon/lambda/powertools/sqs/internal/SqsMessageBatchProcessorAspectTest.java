package software.amazon.lambda.powertools.sqs.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;
import software.amazon.lambda.powertools.sqs.handlers.LambdaHandlerApiGateway;
import software.amazon.lambda.powertools.sqs.handlers.PartialBatchFailureSuppressedHandler;
import software.amazon.lambda.powertools.sqs.handlers.PartialBatchPartialFailureHandler;
import software.amazon.lambda.powertools.sqs.handlers.PartialBatchSuccessHandler;
import software.amazon.lambda.powertools.sqs.handlers.SqsMessageHandlerWithNonRetryableHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.lambda.powertools.sqs.SqsUtils.overrideSqsClient;

public class SqsMessageBatchProcessorAspectTest {
    public static final Random mockedRandom = mock(Random.class);
    private static final SqsClient sqsClient = mock(SqsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SQSEvent event;
    private RequestHandler<SQSEvent, String> requestHandler;

    private final Context context = mock(Context.class);

    @BeforeEach
    void setUp() throws IOException {
        overrideSqsClient(sqsClient);
        reset(mockedRandom);
        reset(sqsClient);
        setupContext();
        event = MAPPER.readValue(this.getClass().getResource("/sampleSqsBatchEvent.json"), SQSEvent.class);
        requestHandler = new PartialBatchSuccessHandler();
    }

    @Test
    void shouldBatchProcessAllMessageSuccessfullyAndNotDeleteFromSQS() {
        requestHandler.handleRequest(event, context);

        verify(mockedRandom, times(2)).nextInt();
        verify(sqsClient, times(0)).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    @Test
    void shouldBatchProcessMessageWithSuccessDeletedOnFailureInBatchFromSQS() {
        requestHandler = new PartialBatchPartialFailureHandler();

        assertThatExceptionOfType(SQSBatchProcessingException.class)
                .isThrownBy(() -> requestHandler.handleRequest(event, context))
                .satisfies(e -> {
                    assertThat(e.getExceptions())
                            .hasSize(1)
                            .extracting("detailMessage")
                            .containsExactly("2e1424d4-f796-459a-8184-9c92662be6da");

                    assertThat(e.getFailures())
                            .hasSize(1)
                            .extracting("messageId")
                            .containsExactly("2e1424d4-f796-459a-8184-9c92662be6da");

                    assertThat(e.successMessageReturnValues())
                            .hasSize(1)
                            .contains("Success");
                });

        verify(mockedRandom).nextInt();
        verify(sqsClient).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    @Test
    void shouldBatchProcessMessageWithSuccessDeletedOnFailureWithSuppressionInBatchFromSQS() {
        requestHandler = new PartialBatchFailureSuppressedHandler();

        requestHandler.handleRequest(event, context);

        verify(mockedRandom).nextInt();
        verify(sqsClient).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    @Test
    void shouldNotTakeEffectOnNonSqsEventHandler() {
        LambdaHandlerApiGateway handlerApiGateway = new LambdaHandlerApiGateway();

        handlerApiGateway.handleRequest(mock(APIGatewayProxyRequestEvent.class), context);

        verifyNoInteractions(sqsClient);
    }

    @Test
    void shouldBatchProcessAndMoveNonRetryableExceptionToDlq() {
        requestHandler = new SqsMessageHandlerWithNonRetryableHandler();
        event.getRecords().get(0).setMessageId("");

        HashMap<QueueAttributeName, String> attributes = new HashMap<>();

        attributes.put(QueueAttributeName.REDRIVE_POLICY, "{\n" +
                "  \"deadLetterTargetArn\": \"arn:aws:sqs:us-east-2:123456789012:retry-queue\",\n" +
                "  \"maxReceiveCount\": 2\n" +
                "}");

        when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(GetQueueAttributesResponse.builder()
                .attributes(attributes)
                .build());

        requestHandler.handleRequest(event, context);

        verify(mockedRandom).nextInt();
        verify(sqsClient).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
        verify(sqsClient).sendMessageBatch(any(Consumer.class));
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}