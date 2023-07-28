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

package software.amazon.lambda.powertools.sqs.internal;

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
import static software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect.FailedProcessingLargePayloadException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import software.amazon.lambda.powertools.sqs.SqsUtils;
import software.amazon.lambda.powertools.sqs.handlers.LambdaHandlerApiGateway;
import software.amazon.lambda.powertools.sqs.handlers.SqsMessageHandler;
import software.amazon.lambda.powertools.sqs.handlers.SqsNoDeleteMessageHandler;

public class SqsLargeMessageAspectTest {

    private static final String BUCKET_NAME = "bucketname";
    private static final String BUCKET_KEY = "c71eb2ae-37e0-4265-8909-32f4153faddf";
    private RequestHandler<SQSEvent, String> requestHandler;
    @Mock
    private Context context;
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
        setupContext();
        SqsUtils.overrideS3Client(s3Client);
        requestHandler = new SqsMessageHandler();
    }

    @Test
    public void testLargeMessage() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSEvent sqsEvent = messageWithBody(
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        String response = requestHandler.handleRequest(sqsEvent, context);

        assertThat(response)
                .isEqualTo("A big message");

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

    @Test
    public void shouldNotProcessSmallMessageBody() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());

        SQSEvent sqsEvent = messageWithBody("This is small message");

        String response = requestHandler.handleRequest(sqsEvent, context);

        assertThat(response)
                .isEqualTo("This is small message");

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

        assertThatExceptionOfType(FailedProcessingLargePayloadException.class)
                .isThrownBy(() -> requestHandler.handleRequest(sqsEvent, context))
                .withCause(exception);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    public void testLargeMessageWithDeletionOff() {
        requestHandler = new SqsNoDeleteMessageHandler();

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3ObjectWithLargeMessage());
        SQSEvent sqsEvent = messageWithBody(
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]");

        String response = requestHandler.handleRequest(sqsEvent, context);

        assertThat(response).isEqualTo("A big message");

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

        assertThatExceptionOfType(FailedProcessingLargePayloadException.class)
                .isThrownBy(() -> requestHandler.handleRequest(sqsEvent, context))
                .withCauseInstanceOf(IOException.class);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    public void shouldNotDoAnyProcessingWhenNotSqsEvent() {
        LambdaHandlerApiGateway handler = new LambdaHandlerApiGateway();

        String messageBody =
                "[\"software.amazon.payloadoffloading.PayloadS3Pointer\",{\"s3BucketName\":\"" + BUCKET_NAME +
                        "\",\"s3Key\":\"" + BUCKET_KEY + "\"}]";

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(messageBody);
        String response = handler.handleRequest(event, context);

        assertThat(response)
                .isEqualTo(messageBody);

        verifyNoInteractions(s3Client);
    }

    private ResponseInputStream<GetObjectResponse> s3ObjectWithLargeMessage() {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream("A big message".getBytes())));
    }

    private SQSEvent messageWithBody(String messageBody) {
        SQSMessage sqsMessage = new SQSMessage();
        sqsMessage.setBody(messageBody);
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(singletonList(sqsMessage));
        return sqsEvent;
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}