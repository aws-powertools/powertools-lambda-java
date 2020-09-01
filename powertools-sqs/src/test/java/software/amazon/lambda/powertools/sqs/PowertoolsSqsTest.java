package software.amazon.lambda.powertools.sqs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import software.amazon.lambda.powertools.sqs.internal.SqsMessageAspect;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class PowertoolsSqsTest {

    @Mock
    private AmazonS3 amazonS3;
    private static final String BUCKET_NAME = "ms-extended-sqs-client";
    private static final String BUCKET_KEY = "c71eb2ae-37e0-4265-8909-32f4153faddf";

    @BeforeEach
    void setUp() throws IllegalAccessException {
        initMocks(this);
        writeStaticField(SqsMessageAspect.class, "amazonS3", amazonS3, true);
    }

    @Test
    public void testLargeMessage() {
        S3Object s3Response = new S3Object();
        s3Response.setObjectContent(new ByteArrayInputStream("A big message".getBytes()));

        when(amazonS3.getObject(BUCKET_NAME, BUCKET_KEY)).thenReturn(s3Response);
        SQSEvent sqsEvent = messageWithBody("[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME + "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        Map<String, String> sqsMessage = PowertoolsSqs.enrichedMessageFromS3(sqsEvent, sqsMessages -> {
            Map<String, String> someBusinessLogic = new HashMap<>();
            someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
            return someBusinessLogic;
        });

        assertThat(sqsMessage)
                .hasSize(1)
                .containsEntry("Message", "A big message");

        verify(amazonS3).deleteObject(BUCKET_NAME, BUCKET_KEY);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testLargeMessageDeleteFromS3Toggle(boolean deleteS3Payload) {
        S3Object s3Response = new S3Object();
        s3Response.setObjectContent(new ByteArrayInputStream("A big message".getBytes()));

        when(amazonS3.getObject(BUCKET_NAME, BUCKET_KEY)).thenReturn(s3Response);
        SQSEvent sqsEvent = messageWithBody("[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME + "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        Map<String, String> sqsMessage = PowertoolsSqs.enrichedMessageFromS3(sqsEvent, deleteS3Payload, sqsMessages -> {
            Map<String, String> someBusinessLogic = new HashMap<>();
            someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
            return someBusinessLogic;
        });

        assertThat(sqsMessage)
                .hasSize(1)
                .containsEntry("Message", "A big message");
        if (deleteS3Payload) {
            verify(amazonS3).deleteObject(BUCKET_NAME, BUCKET_KEY);
        } else {
            verify(amazonS3, never()).deleteObject(BUCKET_NAME, BUCKET_KEY);
        }
    }

    @Test
    public void shouldNotProcessSmallMessageBody() {
        S3Object s3Response = new S3Object();
        s3Response.setObjectContent(new ByteArrayInputStream("A big message".getBytes()));

        when(amazonS3.getObject(BUCKET_NAME, BUCKET_KEY)).thenReturn(s3Response);
        SQSEvent sqsEvent = messageWithBody("This is small message");

        Map<String, String> sqsMessage = PowertoolsSqs.enrichedMessageFromS3(sqsEvent, sqsMessages -> {
            Map<String, String> someBusinessLogic = new HashMap<>();
            someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
            return someBusinessLogic;
        });

        assertThat(sqsMessage)
                .containsEntry("Message", "This is small message");

        verifyNoInteractions(amazonS3);
    }

    @ParameterizedTest
    @MethodSource("exception")
    public void shouldFailEntireBatchIfFailedDownloadingFromS3(RuntimeException exception) {
        when(amazonS3.getObject(BUCKET_NAME, BUCKET_KEY)).thenThrow(exception);

        String messageBody = "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME + "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]";
        SQSEvent sqsEvent = messageWithBody(messageBody);

        assertThatExceptionOfType(SqsMessageAspect.FailedProcessingLargePayloadException.class)
                .isThrownBy(() -> PowertoolsSqs.enrichedMessageFromS3(sqsEvent, sqsMessages -> sqsMessages.get(0).getBody()))
                .withCause(exception);

        verify(amazonS3, never()).deleteObject(BUCKET_NAME, BUCKET_KEY);
    }

    @Test
    public void shouldFailEntireBatchIfFailedProcessingDownloadMessageFromS3() throws IOException {
        S3Object s3Response = new S3Object();

        s3Response.setObjectContent(new S3ObjectInputStream(new StringInputStream("test") {
            @Override
            public void close() throws IOException {
                throw new IOException("Failed");
            }
        }, mock(HttpRequestBase.class)));

        when(amazonS3.getObject(BUCKET_NAME, BUCKET_KEY)).thenReturn(s3Response);

        String messageBody = "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME + "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]";
        SQSEvent sqsEvent = messageWithBody(messageBody);

        assertThatExceptionOfType(SqsMessageAspect.FailedProcessingLargePayloadException.class)
                .isThrownBy(() -> PowertoolsSqs.enrichedMessageFromS3(sqsEvent, sqsMessages -> sqsMessages.get(0).getBody()))
                .withCauseInstanceOf(IOException.class);

        verify(amazonS3, never()).deleteObject(BUCKET_NAME, BUCKET_KEY);
    }

    private static Stream<Arguments> exception() {
        return Stream.of(Arguments.of(new AmazonServiceException("Service Exception")),
                Arguments.of(new SdkClientException("Client Exception")));
    }

    private SQSEvent messageWithBody(String messageBody) {
        SQSMessage sqsMessage = new SQSMessage();
        sqsMessage.setBody(messageBody);
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(singletonList(sqsMessage));
        return sqsEvent;
    }
}