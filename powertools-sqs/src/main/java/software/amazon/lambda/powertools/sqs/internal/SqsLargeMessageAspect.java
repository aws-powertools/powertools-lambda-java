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
import static java.lang.String.format;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.sqs.SqsUtils.s3Client;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.lambda.powertools.sqs.SqsLargeMessage;
import software.amazon.payloadoffloading.PayloadS3Pointer;

@Aspect
public class SqsLargeMessageAspect {

    private static final Logger LOG = LoggerFactory.getLogger(SqsLargeMessageAspect.class);

    public static List<PayloadS3Pointer> processMessages(final List<SQSMessage> records) {
        List<PayloadS3Pointer> s3Pointers = new ArrayList<>();
        for (SQSMessage sqsMessage : records) {
            if (isBodyLargeMessagePointer(sqsMessage.getBody())) {

                PayloadS3Pointer s3Pointer = Optional.ofNullable(PayloadS3Pointer.fromJson(sqsMessage.getBody()))
                        .orElseThrow(() -> new FailedProcessingLargePayloadException(
                                format("Failed processing SQS body to extract S3 details. [ %s ].",
                                        sqsMessage.getBody())));

                ResponseInputStream<GetObjectResponse> s3Object = callS3Gracefully(s3Pointer, pointer ->
                {
                    ResponseInputStream<GetObjectResponse> response =
                            s3Client().getObject(GetObjectRequest.builder()
                                    .bucket(pointer.getS3BucketName())
                                    .key(pointer.getS3Key())
                                    .build());

                    LOG.debug("Object downloaded with key: " + s3Pointer.getS3Key());
                    return response;
                });

                sqsMessage.setBody(readStringFromS3Object(s3Object, s3Pointer));
                s3Pointers.add(s3Pointer);
            }
        }

        return s3Pointers;
    }

    private static boolean isBodyLargeMessagePointer(String record) {
        return record.startsWith("[\"software.amazon.payloadoffloading.PayloadS3Pointer\"");
    }

    private static String readStringFromS3Object(ResponseInputStream<GetObjectResponse> response,
                                                 PayloadS3Pointer s3Pointer) {
        try (ResponseInputStream<GetObjectResponse> content = response) {
            return IoUtils.toUtf8String(content);
        } catch (IOException e) {
            LOG.error("Error converting S3 object to String", e);
            throw new FailedProcessingLargePayloadException(
                    format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]",
                            s3Pointer.getS3BucketName(), s3Pointer.getS3Key()), e);
        }
    }

    public static void deleteMessage(PayloadS3Pointer s3Pointer) {
        callS3Gracefully(s3Pointer, pointer ->
        {
            s3Client().deleteObject(DeleteObjectRequest.builder()
                    .bucket(pointer.getS3BucketName())
                    .key(pointer.getS3Key())
                    .build());
            LOG.info("Message deleted from S3: " + s3Pointer.toJson());
            return null;
        });
    }

    private static <R> R callS3Gracefully(final PayloadS3Pointer pointer,
                                          final Function<PayloadS3Pointer, R> function) {
        try {
            return function.apply(pointer);
        } catch (S3Exception e) {
            LOG.error("A service exception", e);
            throw new FailedProcessingLargePayloadException(
                    format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]",
                            pointer.getS3BucketName(), pointer.getS3Key()), e);
        } catch (SdkClientException e) {
            LOG.error("Some sort of client exception", e);
            throw new FailedProcessingLargePayloadException(
                    format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]",
                            pointer.getS3BucketName(), pointer.getS3Key()), e);
        }
    }

    public static boolean placedOnSqsEventRequestHandler(ProceedingJoinPoint pjp) {
        return pjp.getArgs().length == 2
                && pjp.getArgs()[0] instanceof SQSEvent
                && pjp.getArgs()[1] instanceof Context;
    }

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(sqsLargeMessage)")
    public void callAt(SqsLargeMessage sqsLargeMessage) {
    }

    @Around(value = "callAt(sqsLargeMessage) && execution(@SqsLargeMessage * *.*(..))", argNames = "pjp,sqsLargeMessage")
    public Object around(ProceedingJoinPoint pjp,
                         SqsLargeMessage sqsLargeMessage) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)
                && placedOnSqsEventRequestHandler(pjp)) {
            List<PayloadS3Pointer> pointersToDelete = rewriteMessages((SQSEvent) proceedArgs[0]);

            Object proceed = pjp.proceed(proceedArgs);

            if (sqsLargeMessage.deletePayloads()) {
                pointersToDelete.forEach(SqsLargeMessageAspect::deleteMessage);
            }
            return proceed;
        }

        return pjp.proceed(proceedArgs);
    }

    private List<PayloadS3Pointer> rewriteMessages(SQSEvent sqsEvent) {
        List<SQSMessage> records = sqsEvent.getRecords();
        return processMessages(records);
    }

    public static class FailedProcessingLargePayloadException extends RuntimeException {
        public FailedProcessingLargePayloadException(String message, Throwable cause) {
            super(message, cause);
        }

        public FailedProcessingLargePayloadException(String message) {
            super(message);
        }
    }
}
