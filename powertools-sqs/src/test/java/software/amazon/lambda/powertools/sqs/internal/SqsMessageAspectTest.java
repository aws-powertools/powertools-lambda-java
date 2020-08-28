package software.amazon.lambda.powertools.sqs.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.lambda.powertools.sqs.handlers.SqsMessageHandler;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SqsMessageAspectTest {

    private RequestHandler<SQSEvent, String> requestHandler;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        initMocks(this);
        setupContext();
        requestHandler = new SqsMessageHandler();
    }

    @Test
    public void testLargeMessage() {
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody("[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"ms-extended-sqs-client\",\"s3Key\":\"c71eb2ae-37e0-4265-8909-32f4153faddf\"}]");
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(Arrays.asList(sqsMessage));
        String response = requestHandler.handleRequest(sqsEvent, context);

//        assertThat(response).hasToString("newValueFromS3");
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}