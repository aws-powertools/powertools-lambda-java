package software.amazon.lambda.powertools.largemessages.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.lambda.powertools.largemessages.LargeMessageConfig;
import software.amazon.lambda.powertools.largemessages.LargeMessageProcessingException;
import software.amazon.payloadoffloading.PayloadS3Pointer;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * Abstract processor for Large Messages. Handle the download from S3 and replace the actual S3 pointer with the content
 * of the S3 Object leveraging the payloadoffloading library.
 *
 * @param <T> any message type that support Large Messages with S3 pointers
 *           ({@link com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage} and {@link com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord} at the moment)
 */
public abstract class LargeMessageProcessor<T> {

    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageProcessor.class);
    private final S3Client s3Client = LargeMessageConfig.get().getS3Client();

    public Object process(ProceedingJoinPoint pjp, boolean deleteS3Objects) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        T message = (T) proceedArgs[0];

        String messageContent = getMessageContent(message);

        if (messageContent == null || !isBodyLargeMessagePointer(messageContent)) {
            LOG.warn("No content in the message or not a large message, proceeding");
            return pjp.proceed(proceedArgs);
        }

        PayloadS3Pointer s3Pointer = Optional.ofNullable(PayloadS3Pointer.fromJson(messageContent))
                .orElseThrow(() -> new LargeMessageProcessingException(format("Failed processing message body to extract S3 details. [ %s ].", messageContent)));

        String s3ObjectContent = getS3ObjectContent(s3Pointer);

        updateMessageContent(message, s3ObjectContent);

        Object response = pjp.proceed(proceedArgs);

        if (deleteS3Objects) {
            deleteS3Object(s3Pointer);
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
     * @param message the message itself
     * @param messageContent the new content of the message (String format)
     */
    protected abstract void updateMessageContent(T message, String messageContent);

    private String getS3ObjectContent(PayloadS3Pointer s3Pointer) {
        ResponseInputStream<GetObjectResponse> s3Object = callS3Gracefully(s3Pointer, pointer -> {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(pointer.getS3BucketName())
                    .key(pointer.getS3Key())
                    .build());

            LOG.debug(format("Object downloaded with key: %s", s3Pointer.getS3Key()));
            return response;
        });

        return readStringFromS3Object(s3Object, s3Pointer);
    }

    private <R> R callS3Gracefully(final PayloadS3Pointer pointer,
                                   final Function<PayloadS3Pointer, R> function) {
        try {
            return function.apply(pointer);
        } catch (SdkException e) {
            throw new LargeMessageProcessingException(format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]", pointer.getS3BucketName(), pointer.getS3Key()), e);
        }
    }

    private String readStringFromS3Object(ResponseInputStream<GetObjectResponse> response,
                                          PayloadS3Pointer s3Pointer) {
        try (ResponseInputStream<GetObjectResponse> content = response) {
            return IoUtils.toUtf8String(content);
        } catch (IOException e) {
            throw new LargeMessageProcessingException(format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s], unable to get S3 object content", s3Pointer.getS3BucketName(), s3Pointer.getS3Key()), e);
        }
    }

    private void deleteS3Object(PayloadS3Pointer s3Pointer) {
        callS3Gracefully(s3Pointer, pointer -> {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(pointer.getS3BucketName())
                    .key(pointer.getS3Key())
                    .build());
            LOG.info(format("Message deleted from S3: %s", s3Pointer.toJson()));
            return null;
        });
    }
}
