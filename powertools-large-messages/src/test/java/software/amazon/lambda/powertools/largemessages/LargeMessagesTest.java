/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.largemessages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class LargeMessagesTest {

    private static final String BIG_MSG = "A biiiiiiiig message";
    private static final String BUCKET_NAME = "bucketname";
    private static final String BUCKET_KEY = "c71eb2ae-37e0-4265-8909-32f4153faddf";
    private static final String BIG_MESSAGE_BODY = "[\"software.amazon.payloadoffloading.PayloadS3Pointer\", {\"s3BucketName\":\""
            + BUCKET_NAME +
            "\", \"s3Key\":\"" + BUCKET_KEY + "\"}]";

    @Mock
    private S3Client s3Client;

    @BeforeEach
    void init() throws NoSuchFieldException, IllegalAccessException {
        // need to clean the s3Client with introspection (singleton)
        Field client = LargeMessageConfig.class.getDeclaredField("s3Client");
        client.setAccessible(true);
        client.set(LargeMessageConfig.get(), null);
        LargeMessageConfig.init().withS3Client(s3Client);
    }

    @Test
    void testProcessLargeSQSMessage_shouldRetrieveFromS3AndDelete() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);

        // when
        String result = LargeMessages.processLargeMessage(sqsMessage, SQSMessage::getBody);

        // then
        assertThat(result).isEqualTo(BIG_MSG);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(delete.capture());
        assertThat(delete.getValue().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(delete.getValue().key()).isEqualTo(BUCKET_KEY);
    }

    @Test
    void testProcessLargeSQSMessage_withDeleteDisabled_shouldNotDelete() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);

        // when
        String result = LargeMessages.processLargeMessage(sqsMessage, SQSMessage::getBody, false);

        // then
        assertThat(result).isEqualTo(BIG_MSG);
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testProcessLargeSNSMessage_shouldRetrieveFromS3AndDelete() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SNSRecord snsRecord = snsRecordWithMessage(BIG_MESSAGE_BODY, true);

        // when
        String result = LargeMessages.processLargeMessage(snsRecord, msg -> msg.getSNS().getMessage());

        // then
        assertThat(result).isEqualTo(BIG_MSG);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testProcessSmallMessage_shouldNotInteractWithS3() {
        // given
        SQSMessage sqsMessage = sqsMessageWithBody("Small message", false);

        // when
        String result = LargeMessages.processLargeMessage(sqsMessage, SQSMessage::getBody);

        // then
        assertThat(result).isEqualTo("Small message");
        verifyNoInteractions(s3Client);
    }

    @Test
    void testProcessUnsupportedMessageType_shouldCallHandlerDirectly() {
        // given
        KinesisEventRecord kinesisRecord = new KinesisEventRecord();
        kinesisRecord.setEventID("kinesis-123");

        // when
        String result = LargeMessages.processLargeMessage(kinesisRecord, KinesisEventRecord::getEventID);

        // then
        assertThat(result).isEqualTo("kinesis-123");
        verifyNoInteractions(s3Client);
    }

    @Test
    void testProcessMessageWithNullBody_shouldCallHandler() {
        // given
        SQSMessage sqsMessage = sqsMessageWithBody(null, true);

        // when
        String result = LargeMessages.processLargeMessage(sqsMessage, SQSMessage::getBody);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(s3Client);
    }

    @Test
    void testProcessMessage_whenS3GetFails_shouldThrowException() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.create("Access denied", new Exception("Permission denied")));
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);

        // when / then
        assertThatThrownBy(() -> LargeMessages.processLargeMessage(sqsMessage, SQSMessage::getBody))
                .isInstanceOf(LargeMessageProcessingException.class)
                .hasMessageContaining("Failed processing S3 record");
    }

    @Test
    void testProcessMessage_whenS3DeleteFails_shouldThrowException() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.create("Access denied", new Exception("Permission denied")));
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);

        // when / then
        assertThatThrownBy(() -> LargeMessages.processLargeMessage(sqsMessage, SQSMessage::getBody))
                .isInstanceOf(LargeMessageProcessingException.class)
                .hasMessageContaining("Failed deleting S3 record");
    }

    @Test
    void testProcessMessage_whenHandlerThrowsRuntimeException_shouldPropagate() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);

        // when / then
        assertThatThrownBy(() -> LargeMessages.processLargeMessage(sqsMessage, msg -> {
            throw new IllegalStateException("Handler error");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Handler error");
    }

    @Test
    void testProcessLargeMessage_withMultiParam_shouldRetrieveFromS3AndDelete() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);
        String orderId = "order-123";

        // when
        String result = LargeMessages.processLargeMessage(sqsMessage, msg -> processOrderSimple(msg, orderId));

        // then
        assertThat(result).isEqualTo("order-123-processed");
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testProcessLargeMessage_withMultiParamAndDeleteDisabled_shouldNotDelete() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);
        String orderId = "order-456";

        // when
        String result = LargeMessages.processLargeMessage(sqsMessage, msg -> processOrderSimple(msg, orderId), false);

        // then
        assertThat(result).isEqualTo("order-456-processed");
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testProcessLargeMessage_shouldModifyMessageInPlace() {
        // given
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSMessage sqsMessage = sqsMessageWithBody(BIG_MESSAGE_BODY, true);
        String originalBody = sqsMessage.getBody();

        // when
        LargeMessages.processLargeMessage(sqsMessage, msg -> {
            assertThat(msg.getBody()).isEqualTo(BIG_MSG);
            return null;
        });

        // then - verify the original message object was modified
        assertThat(sqsMessage.getBody()).isEqualTo(BIG_MSG);
        assertThat(sqsMessage.getBody()).isNotEqualTo(originalBody);
    }

    private String processOrderSimple(SQSMessage message, String orderId) {
        assertThat(message.getBody()).isEqualTo(BIG_MSG);
        return orderId + "-processed";
    }

    private ResponseInputStream<GetObjectResponse> s3ObjectWithLargeMessage() {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(BIG_MSG.getBytes())));
    }

    private SQSMessage sqsMessageWithBody(String messageBody, boolean largeMessage) {
        SQSMessage sqsMessage = new SQSMessage();
        sqsMessage.setBody(messageBody);
        if (messageBody != null) {
            sqsMessage.setMd5OfBody("dummy-md5");
        }

        if (largeMessage) {
            Map<String, MessageAttribute> attributeMap = new HashMap<>();
            MessageAttribute payloadAttribute = new MessageAttribute();
            payloadAttribute.setStringValue("300450");
            payloadAttribute.setDataType("Number");
            attributeMap.put("ExtendedPayloadSize", payloadAttribute);

            sqsMessage.setMessageAttributes(attributeMap);
            sqsMessage.setMd5OfMessageAttributes("dummy-md5");
        }
        return sqsMessage;
    }

    private SNSRecord snsRecordWithMessage(String messageBody, boolean largeMessage) {
        SNS sns = new SNS().withMessage(messageBody);
        if (largeMessage) {
            sns.setMessageAttributes(Collections.singletonMap("ExtendedPayloadSize",
                    new SNSEvent.MessageAttribute()));
        }
        return new SNSRecord().withSns(sns);
    }
}
