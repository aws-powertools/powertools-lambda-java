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

package software.amazon.lambda.powertools.sqs;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect;

class SqsUtilsLargeMessageTest {

    private static final String BUCKET_NAME = "ms-extended-sqs-client";
    private static final String BUCKET_KEY = "c71eb2ae-37e0-4265-8909-32f4153faddf";
    @Mock
    private S3Client s3Client;

    private static Stream<Arguments> exception() {
        return Stream.of(Arguments.of(S3Exception.builder()
                        .message("Service Exception")
                        .build()),
                Arguments.of(SdkClientException.builder()
                        .message("Client Exception")
                        .build()));
    }

    @BeforeEach
    void setUp() {
        openMocks(this);
        SqsUtils.overrideS3Client(s3Client);
    }

    @Test
    public void testLargeMessage() {
        ResponseInputStream<GetObjectResponse> s3Response =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream("A big message".getBytes())));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Response);

        SQSEvent sqsEvent = messageWithBody(
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        Map<String, String> sqsMessage = SqsUtils.enrichedMessageFromS3(sqsEvent, sqsMessages ->
        {
            Map<String, String> someBusinessLogic = new HashMap<>();
            someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
            return someBusinessLogic;
        });

        assertThat(sqsMessage)
                .hasSize(1)
                .containsEntry("Message", "A big message");

        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(s3Client).deleteObject(delete.capture());

        Assertions.assertThat(delete.getValue())
                .satisfies((Consumer<DeleteObjectRequest>) deleteObjectRequest ->
                {
                    assertThat(deleteObjectRequest.bucket())
                            .isEqualTo(BUCKET_NAME);

                    assertThat(deleteObjectRequest.key())
                            .isEqualTo(BUCKET_KEY);
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testLargeMessageDeleteFromS3Toggle(boolean deleteS3Payload) {
        ResponseInputStream<GetObjectResponse> s3Response =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream("A big message".getBytes())));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Response);

        SQSEvent sqsEvent = messageWithBody(
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        Map<String, String> sqsMessage = SqsUtils.enrichedMessageFromS3(sqsEvent, deleteS3Payload, sqsMessages ->
        {
            Map<String, String> someBusinessLogic = new HashMap<>();
            someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
            return someBusinessLogic;
        });

        assertThat(sqsMessage)
                .hasSize(1)
                .containsEntry("Message", "A big message");
        if (deleteS3Payload) {
            ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);

            verify(s3Client).deleteObject(delete.capture());

            Assertions.assertThat(delete.getValue())
                    .satisfies((Consumer<DeleteObjectRequest>) deleteObjectRequest ->
                    {
                        assertThat(deleteObjectRequest.bucket())
                                .isEqualTo(BUCKET_NAME);

                        assertThat(deleteObjectRequest.key())
                                .isEqualTo(BUCKET_KEY);
                    });
        } else {
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }
    }

    @Test
    public void shouldNotProcessSmallMessageBody() {
        ResponseInputStream<GetObjectResponse> s3Response =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream("A big message".getBytes())));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Response);

        SQSEvent sqsEvent = messageWithBody("This is small message");

        Map<String, String> sqsMessage = SqsUtils.enrichedMessageFromS3(sqsEvent, sqsMessages ->
        {
            Map<String, String> someBusinessLogic = new HashMap<>();
            someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
            return someBusinessLogic;
        });

        assertThat(sqsMessage)
                .containsEntry("Message", "This is small message");

        verifyNoInteractions(s3Client);
    }

    @ParameterizedTest
    @MethodSource("exception")
    public void shouldFailEntireBatchIfFailedDownloadingFromS3(RuntimeException exception) {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(exception);

        String messageBody =
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]";
        SQSEvent sqsEvent = messageWithBody(messageBody);

        assertThatExceptionOfType(SqsLargeMessageAspect.FailedProcessingLargePayloadException.class)
                .isThrownBy(() -> SqsUtils.enrichedMessageFromS3(sqsEvent, sqsMessages -> sqsMessages.get(0).getBody()))
                .withCause(exception);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    public void shouldFailEntireBatchIfFailedProcessingDownloadMessageFromS3() {
        ResponseInputStream<GetObjectResponse> s3Response =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new StringInputStream("test") {
                            @Override
                            public void close() throws IOException {
                                throw new IOException("Failed");
                            }
                        }));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Response);

        String messageBody =
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]";
        SQSEvent sqsEvent = messageWithBody(messageBody);

        assertThatExceptionOfType(SqsLargeMessageAspect.FailedProcessingLargePayloadException.class)
                .isThrownBy(() -> SqsUtils.enrichedMessageFromS3(sqsEvent, sqsMessages -> sqsMessages.get(0).getBody()))
                .withCauseInstanceOf(IOException.class);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    private SQSEvent messageWithBody(String messageBody) {
        SQSMessage sqsMessage = new SQSMessage();
        sqsMessage.setBody(messageBody);
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(singletonList(sqsMessage));
        return sqsEvent;
    }
}