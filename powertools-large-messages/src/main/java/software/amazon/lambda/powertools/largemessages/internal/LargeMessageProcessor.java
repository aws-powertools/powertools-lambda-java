package software.amazon.lambda.powertools.largemessages.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.lambda.powertools.largemessages.LargeMessageConfig;
import software.amazon.lambda.powertools.largemessages.LargeMessageProcessingException;
import software.amazon.payloadoffloading.S3BackedPayloadStore;
import software.amazon.payloadoffloading.S3Dao;

import static java.lang.String.format;

/**
 * Abstract processor for Large Messages. Handle the download from S3 and replace the actual S3 pointer with the content
 * of the S3 Object leveraging the payloadoffloading library.
 *
 * @param <T> any message type that support Large Messages with S3 pointers
 *            ({@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage} and {@link com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord} at the moment)
 */
public abstract class LargeMessageProcessor<T> {

    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageProcessor.class);
    private final S3Client s3Client = LargeMessageConfig.get().getS3Client();
    private final S3BackedPayloadStore payloadStore = new S3BackedPayloadStore(new S3Dao(s3Client), "DUMMY");

    public Object process(ProceedingJoinPoint pjp, boolean deleteS3Object) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        T message = (T) proceedArgs[0];

        String payloadPointer = getMessageContent(message);

        if (payloadPointer == null || !isBodyLargeMessagePointer(payloadPointer)) {
            LOG.warn("No content in the message or not a large message, proceeding");
            return pjp.proceed(proceedArgs);
        }

        String s3ObjectContent = getS3ObjectContent(payloadPointer);

        updateMessageContent(message, s3ObjectContent);

        Object response = pjp.proceed(proceedArgs);

        if (deleteS3Object) {
            deleteS3Object(payloadPointer);
        }

        return response;
    }

    protected boolean isBodyLargeMessagePointer(String messageBody) {
        return messageBody.startsWith("[\"software.amazon.payloadoffloading.PayloadS3Pointer\"");
    }

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
            // TODO: should we actually throw an exception if deletion failed ?
            throw new LargeMessageProcessingException(format("Failed deleting S3 record [%s]", payloadPointer), e);
        }
    }
}
