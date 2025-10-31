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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.lambda.powertools.largemessages.LargeMessageConfig;
import software.amazon.lambda.powertools.largemessages.LargeMessageProcessingException;
import software.amazon.payloadoffloading.S3BackedPayloadStore;
import software.amazon.payloadoffloading.S3Dao;

/**
 * Abstract processor for Large Messages.
 * <p>
 * Handles the download from S3 and replaces the S3 pointer with the actual content
 * of the S3 Object, leveraging the payloadoffloading library.
 *
 * @param <T> any message type that supports Large Messages with S3 pointers
 *            ({@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage}
 *            and {@link com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord})
 */
public abstract class LargeMessageProcessor<T> {
    protected static final String RESERVED_ATTRIBUTE_NAME = "ExtendedPayloadSize";
    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageProcessor.class);

    private final S3Client s3Client = LargeMessageConfig.get().getS3Client();
    private final S3BackedPayloadStore payloadStore = new S3BackedPayloadStore(new S3Dao(s3Client), "DUMMY");

    /**
     * Process a large message using a functional interface.
     *
     * @param message the message to process
     * @param function the function to execute with the processed message
     * @param deleteS3Object whether to delete the S3 object after processing
     * @param <R> the return type of the wrapped function
     * @return the result of the function execution
     * @throws Throwable if an error occurs during processing
     */
    public <R> R process(T message, LargeMessageFunction<T, R> function, boolean deleteS3Object) throws Throwable {
        if (!isLargeMessage(message)) {
            LOG.warn("Not a large message, proceeding");
            return function.apply(message);
        }

        String payloadPointer = getMessageContent(message);
        if (payloadPointer == null) {
            LOG.warn("No content in the message, proceeding");
            return function.apply(message);
        }

        // legacy attribute (sqs only)
        payloadPointer = payloadPointer.replace("com.amazon.sqs.javamessaging.MessageS3Pointer",
                "software.amazon.payloadoffloading.PayloadS3Pointer");

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

        R result = function.apply(message);

        if (deleteS3Object) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Large message [{}]: deleting object from S3", getMessageId(message));
            }
            deleteS3Object(payloadPointer);
        }

        return result;
    }

    /**
     * Retrieve the message ID.
     *
     * @param message the message
     * @return the message ID
     */
    protected abstract String getMessageId(T message);

    /**
     * Retrieve the content of the message (e.g., body of an SQSMessage).
     *
     * @param message the message
     * @return the message content
     */
    protected abstract String getMessageContent(T message);

    /**
     * Update the message content (e.g., body of an SQSMessage).
     *
     * @param message        the message
     * @param messageContent the new message content
     */
    protected abstract void updateMessageContent(T message, String messageContent);

    /**
     * Check if the message is a large message (based on message attributes).
     *
     * @param message the message
     * @return true if the message is a large message, false otherwise
     */
    protected abstract boolean isLargeMessage(T message);

    /**
     * Remove the large message indicator from message attributes.
     *
     * @param message the message
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
