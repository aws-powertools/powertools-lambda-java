package software.amazon.lambda.powertools.largemessages.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.lambda.powertools.largemessages.LargeMessage;
import software.amazon.lambda.powertools.largemessages.LargeMessageConfig;
import software.amazon.lambda.powertools.largemessages.LargeMessageProcessingException;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class LargeMessageAspectTest {

    private static final String BIG_MSG = "A biiiiiiiig message";
    private static final String BUCKET_NAME = "bucketname";
    private static final String BUCKET_KEY = "c71eb2ae-37e0-4265-8909-32f4153faddf";
    private static final String BIG_MESSAGE_BODY = "[\"software.amazon.payloadoffloading.PayloadS3Pointer\", {\"s3BucketName\":\"" + BUCKET_NAME + "\", \"s3Key\":\"" + BUCKET_KEY + "\"}]";

    @Mock
    private S3Client s3Client;
    @Mock
    private Context context;

    @BeforeEach
    public void init() throws NoSuchFieldException, IllegalAccessException {
        openMocks(this);
        setupContext();
        // need to clean the s3Client with introspection (singleton)
        Field client = LargeMessageConfig.class.getDeclaredField("s3Client");
        client.setAccessible(true);
        client.set(LargeMessageConfig.get(), null);
        LargeMessageConfig.init().withS3Client(s3Client);
    }

    @LargeMessage
    private String processSQSMessage(SQSMessage sqsMessage, Context context) {
        return sqsMessage.getBody();
    }

    @LargeMessage
    private String processSNSMessageWithoutContext(SNSRecord snsRecord) {
        return snsRecord.getSNS().getMessage();
    }

    @LargeMessage(deleteS3Object = false)
    private String processSQSMessageNoDelete(SQSMessage sqsMessage, Context context) {
        return sqsMessage.getBody();
    }

    @LargeMessage
    private String processKinesisMessage(KinesisEventRecord kinesisEventRecord) {
        return kinesisEventRecord.getEventID();
    }

    @LargeMessage
    private String processNoMessage() {
        return "Hello World";
    }

    @Test
    public void testLargeSQSMessageWithDefaultDeletion() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY);

        // when
        String message = processSQSMessage(sqsMessage, context);

        // then
        assertThat(message).isEqualTo(BIG_MSG);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(delete.capture());
        Assertions.assertThat(delete.getValue())
                .satisfies((Consumer<DeleteObjectRequest>) deleteObjectRequest -> {
                    assertThat(deleteObjectRequest.bucket())
                            .isEqualTo(BUCKET_NAME);

                    assertThat(deleteObjectRequest.key())
                            .isEqualTo(BUCKET_KEY);
                });
    }

    @Test
    public void testLargeSNSMessageWithDefaultDeletion() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SNSRecord snsRecord = snsRecordWithMessage(BIG_MESSAGE_BODY);

        //when
        String message = processSNSMessageWithoutContext(snsRecord);

        // then
        assertThat(message).isEqualTo(BIG_MSG);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(delete.capture());
        Assertions.assertThat(delete.getValue())
                .satisfies((Consumer<DeleteObjectRequest>) deleteObjectRequest -> {
                    assertThat(deleteObjectRequest.bucket())
                            .isEqualTo(BUCKET_NAME);

                    assertThat(deleteObjectRequest.key())
                            .isEqualTo(BUCKET_KEY);
                });
    }

    @Test
    public void testLargeSQSMessageWithNoDeletion_shouldNotDelete() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY);

        // when
        String message = processSQSMessageNoDelete(sqsMessage, context);

        // then
        assertThat(message).isEqualTo(BIG_MSG);
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    public void testKinesisMessage_shouldProceedWithoutS3() {
        // given
        KinesisEventRecord kinesisEventRecord = new KinesisEventRecord();
        kinesisEventRecord.setEventID("kinesis_id1234567890");

        // when
        String message = processKinesisMessage(kinesisEventRecord);

        // then
        assertThat(message).isEqualTo("kinesis_id1234567890");
        verifyNoInteractions(s3Client);
    }

    @Test
    public void testNoMessage_shouldProceedWithoutS3() {
        // when
        String message = processNoMessage();

        // then
        assertThat(message).isEqualTo("Hello World");
        verifyNoInteractions(s3Client);
    }

    @Test
    public void testSmallMessage_shouldProceedWithoutS3() {
        // given
        SQSMessage sqsMessage = sqsMessageWithBody("This is small message");

        // when
        String message = processSQSMessage(sqsMessage, context);

        // then
        assertThat(message)
                .isEqualTo("This is small message");
        verifyNoInteractions(s3Client);
    }

    @Test
    public void testNullMessage_shouldProceedWithoutS3() {
        // given
        SQSMessage sqsMessage = sqsMessageWithBody(null);

        // when
        String message = processSQSMessage(sqsMessage, context);

        // then
        assertThat(message).isNull();
        verifyNoInteractions(s3Client);
    }

    @Test
    public void testGetS3ObjectException_shouldThrowLargeMessageProcessingException() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.create("Permission denied", new Exception("User is not allowed to access bucket " + BUCKET_NAME)));
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY);

        // when / then
        assertThatThrownBy(() -> processSQSMessage(sqsMessage, context))
                .isInstanceOf(LargeMessageProcessingException.class)
                .hasMessage(format("Failed processing S3 record [%s]", BIG_MESSAGE_BODY));
    }

    @Test
    public void testDeleteS3ObjectException_shouldThrowLargeMessageProcessingException() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.create("Permission denied", new Exception("User is not allowed to access bucket " + BUCKET_NAME)));
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY);

        // when / then
        assertThatThrownBy(() -> processSQSMessage(sqsMessage, context))
                .isInstanceOf(LargeMessageProcessingException.class)
                .hasMessage(format("Failed deleting S3 record [%s]", BIG_MESSAGE_BODY));
    }

    private ResponseInputStream<GetObjectResponse> s3ObjectWithLargeMessage() {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(), AbortableInputStream.create(new ByteArrayInputStream(BIG_MSG.getBytes())));
    }

    private SQSMessage sqsMessageWithBody(String messageBody) {
        SQSMessage sqsMessage = new SQSMessage();
        sqsMessage.setBody(messageBody);
        return sqsMessage;
    }

    private SNSRecord snsRecordWithMessage(String messageBody) {
        return new SNSRecord().withSns(new SNS().withMessage(messageBody));
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(1024);
    }
}
