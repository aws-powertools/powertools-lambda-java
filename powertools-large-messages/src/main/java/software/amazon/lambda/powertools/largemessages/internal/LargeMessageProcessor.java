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

package software.amazon.lambda.powertools.largemessages.internal;

import static java.lang.String.format;

import java.nio.charset.StandardCharsets;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.lambda.powertools.largemessages.LargeMessageConfig;
import software.amazon.lambda.powertools.largemessages.LargeMessageProcessingException;
import software.amazon.payloadoffloading.S3BackedPayloadStore;
import software.amazon.payloadoffloading.S3Dao;

/**
 * Abstract processor for Large Messages. Handle the download from S3 and replace the actual S3 pointer with the content
 * of the S3 Object leveraging the payloadoffloading library.
 *
 * @param <T> any message type that support Large Messages with S3 pointers
 *            ({@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage} and {@link com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord} at the moment)
 */
abstract class LargeMessageProcessor<T> {
    protected static final String RESERVED_ATTRIBUTE_NAME = "ExtendedPayloadSize";
    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageProcessor.class);

    private final S3Client s3Client = LargeMessageConfig.get().getS3Client();
    private final S3BackedPayloadStore payloadStore = new S3BackedPayloadStore(new S3Dao(s3Client), "DUMMY");

    public Object process(ProceedingJoinPoint pjp, boolean deleteS3Object) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        T message = (T) proceedArgs[0];

        if (!isLargeMessage(message)) {
            LOG.warn("Not a large message, proceeding");
            return pjp.proceed(proceedArgs);
        }

        String payloadPointer = getMessageContent(message);
        if (payloadPointer == null) {
            LOG.warn("No content in the message, proceeding");
            return pjp.proceed(proceedArgs);
        }
        // legacy attribute (sqs only)
        payloadPointer = payloadPointer.replace("com.amazon.sqs.javamessaging.MessageS3Pointer", "software.amazon.payloadoffloading.PayloadS3Pointer");

        if (LOG.isInfoEnabled()) {
            LOG.info("Large message [{}]: retrieving content from S3", getMessageId(message));
        }

        String s3ObjectContent = getS3ObjectContent(payloadPointer);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Large message [{}] retrieved in S3 [{}]: {}KB", getMessageId(message), payloadPointer,
                    s3ObjectContent.getBytes(StandardCharsets.UTF_8).length / 1024);
        }

        updateMessageContent(message, s3ObjectContent);
        removeLargeMessageAttributes(message);

        Object response = pjp.proceed(proceedArgs);

        if (deleteS3Object) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Large message [{}]: deleting object from S3", getMessageId(message));
            }
            deleteS3Object(payloadPointer);
        }

        return response;
    }

    /**
     * Retrieve the message id
     *
     * @param message the message itself
     * @return the id of the message (String format)
     */
    protected abstract String getMessageId(T message);

    /**
     * Retrieve the content of the message (ex: body of an SQSMessage)
     *
     * @param message the message itself
     * @return the content of the message (String format)
     */
    protected abstract String getMessageContent(T message);

    /**
     * Update the message content of the message (ex: body of an SQSMessage)
     *
     * @param message        the message itself
     * @param messageContent the new content of the message (String format)
     */
    protected abstract void updateMessageContent(T message, String messageContent);

    /**
     * Check if the message is actually a large message (indicator in message attributes)
     *
     * @param message the message itself
     * @return true if the message is a large message
     */
    protected abstract boolean isLargeMessage(T message);

    /**
     * Remove the large message indicator (in message attributes)
     *
     * @param message the message itself
     */
    protected abstract void removeLargeMessageAttributes(T message);

    private String getS3ObjectContent(String payloadPointer) {
        try {
            return payloadStore.getOriginalPayload(payloadPointer);
        } catch (SdkException e) {
            throw new LargeMessageProcessingException(format("Failed processing S3 record [%s]", payloadPointer), e);
        }
    }

    private void deleteS3Object(String payloadPointer) {
        try {
            payloadStore.deleteOriginalPayload(payloadPointer);
        } catch (SdkException e) {
            throw new LargeMessageProcessingException(format("Failed deleting S3 record [%s]", payloadPointer), e);
        }
    }

}
