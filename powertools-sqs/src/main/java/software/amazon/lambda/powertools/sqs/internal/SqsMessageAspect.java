package software.amazon.lambda.powertools.sqs.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.sqs.LargeMessageHandler;
import software.amazon.payloadoffloading.PayloadS3Pointer;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static java.lang.String.format;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;

@Aspect
public class SqsMessageAspect {

    private static final Log LOG = LogFactory.getLog(SqsMessageAspect.class);
    private static AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(largeMessageHandler)")
    public void callAt(LargeMessageHandler largeMessageHandler) {
    }

    @Around(value = "callAt(largeMessageHandler) && execution(@LargeMessageHandler * *.*(..))", argNames = "pjp,largeMessageHandler")
    public Object around(ProceedingJoinPoint pjp,
                         LargeMessageHandler largeMessageHandler) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)
                && placedOnSqsEventRequestHandler(pjp)) {
            List<PayloadS3Pointer> pointersToDelete = rewriteMessages((SQSEvent) proceedArgs[0]);

            Object proceed = pjp.proceed(proceedArgs);

            if (largeMessageHandler.deletePayloads()) {
                pointersToDelete.forEach(this::deleteMessageFromS3);
            }
            return proceed;
        }

        return pjp.proceed(proceedArgs);
    }

    private List<PayloadS3Pointer> rewriteMessages(SQSEvent sqsEvent) {
        List<SQSMessage> records = sqsEvent.getRecords();
        return processMessages(records);
    }

    public static List<PayloadS3Pointer> processMessages(final List<SQSMessage> records) {
        List<PayloadS3Pointer> s3Pointers = new ArrayList<>();
        for (SQSMessage sqsMessage : records) {
            if (isBodyLargeMessagePointer(sqsMessage.getBody())) {
                PayloadS3Pointer s3Pointer = PayloadS3Pointer.fromJson(sqsMessage.getBody());

                S3Object s3Object = callS3Gracefully(s3Pointer, pointer -> {
                    S3Object object = amazonS3.getObject(pointer.getS3BucketName(), pointer.getS3Key());
                    LOG.debug("Object downloaded with key: " + s3Pointer.getS3Key());
                    return object;
                });

                sqsMessage.setBody(readStringFromS3Object(s3Object));
                s3Pointers.add(s3Pointer);
            }
        }

        return s3Pointers;
    }

    private static boolean isBodyLargeMessagePointer(String record) {
        return record.startsWith("[\"software.amazon.payloadoffloading.PayloadS3Pointer\"");
    }

    private static String readStringFromS3Object(S3Object object) {
        try (S3ObjectInputStream is = object.getObjectContent()) {
            return IOUtils.toString(is);
        } catch (IOException e) {
            LOG.error("Error converting S3 object to String", e);
            throw new FailedProcessingLargePayloadException(format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]", object.getBucketName(), object.getKey()), e);
        }
    }

    private void deleteMessageFromS3(PayloadS3Pointer s3Pointer) {
        callS3Gracefully(s3Pointer, pointer -> {
            amazonS3.deleteObject(s3Pointer.getS3BucketName(), s3Pointer.getS3Key());
            LOG.info("Message deleted from S3: " + s3Pointer.toJson());
            return null;
        });
    }

    public static void deleteMessage(PayloadS3Pointer s3Pointer) {
        callS3Gracefully(s3Pointer, pointer -> {
            amazonS3.deleteObject(s3Pointer.getS3BucketName(), s3Pointer.getS3Key());
            LOG.info("Message deleted from S3: " + s3Pointer.toJson());
            return null;
        });
    }

    private static <R> R callS3Gracefully(final PayloadS3Pointer pointer,
                                   final Function<PayloadS3Pointer, R> function) {
        try {
            return function.apply(pointer);
        } catch (AmazonServiceException e) {
            LOG.error("A service exception", e);
            throw new FailedProcessingLargePayloadException(format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]", pointer.getS3BucketName(), pointer.getS3Key()), e);
        } catch (SdkClientException e) {
            LOG.error("Some sort of client exception", e);
            throw new FailedProcessingLargePayloadException(format("Failed processing S3 record with [Bucket Name: %s Bucket Key: %s]", pointer.getS3BucketName(), pointer.getS3Key()), e);
        }
    }

    public static boolean placedOnSqsEventRequestHandler(ProceedingJoinPoint pjp) {
        return pjp.getArgs().length == 2
                && pjp.getArgs()[0] instanceof SQSEvent
                && pjp.getArgs()[1] instanceof Context;
    }

    public static class FailedProcessingLargePayloadException extends RuntimeException {
        public FailedProcessingLargePayloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
