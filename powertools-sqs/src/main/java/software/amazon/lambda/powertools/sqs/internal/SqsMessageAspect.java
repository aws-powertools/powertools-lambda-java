package software.amazon.lambda.powertools.sqs.internal;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
public class SqsMessageAspect {

    private static final Log LOG = LogFactory.getLog(SqsMessageAspect.class);
    private AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(largeMessageHandler)")
    public void callAt(LargeMessageHandler largeMessageHandler) {
    }

    @Around(value = "callAt(largeMessageHandler) && execution(@LargeMessageHandler * *.*(..))", argNames = "pjp,largeMessageHandler")
    public Object around(ProceedingJoinPoint pjp,
                         LargeMessageHandler largeMessageHandler) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        Object[] rewrittenArgs = rewriteMessages(proceedArgs);

        Object proceed = pjp.proceed(rewrittenArgs);

        List<PayloadS3Pointer> payloadS3Pointers = collectPayloadPointers(proceedArgs);
        payloadS3Pointers.forEach(this::deleteMessageFromS3);

        return proceed;
    }

    private List<PayloadS3Pointer> collectPayloadPointers(Object[] args) {
        SQSEvent sqsEvent = (SQSEvent) args[0];

        return sqsEvent.getRecords().stream()
                .filter(record -> isBodyLargeMessagePointer(record.getBody()))
                .map(record -> PayloadS3Pointer.fromJson(record.getBody()))
                .collect(Collectors.toList());
    }

    private Object[] rewriteMessages(Object[] args) {
        SQSEvent sqsEvent = (SQSEvent) args[0];

        for (SQSEvent.SQSMessage sqsMessage : sqsEvent.getRecords()) {
            if (isBodyLargeMessagePointer(sqsMessage.getBody())) {
                PayloadS3Pointer s3Pointer = PayloadS3Pointer.fromJson(sqsMessage.getBody());

                try {
                    S3Object object = amazonS3.getObject(s3Pointer.getS3BucketName(), s3Pointer.getS3Key());
                    sqsMessage.setBody(readStringFromS3Object(object));
                    LOG.debug("Object downloaded with key: " + s3Pointer.getS3Key());
                } catch (AmazonServiceException e) {
                    LOG.error("A service exception", e);
                } catch (SdkClientException e) {
                    LOG.error("Some sort of client exception", e);
                }
            }
        }

        args[0] = sqsEvent;

        return args;
    }

    private boolean isBodyLargeMessagePointer(String record) {
        return record.startsWith("[\"software.amazon.payloadoffloading.PayloadS3Pointer\"");
    }

    private String readStringFromS3Object(S3Object object) {
        S3ObjectInputStream is = object.getObjectContent();
        String s3Body = null;
        try {
            s3Body = IOUtils.toString(is);
        } catch (IOException e) {
            LOG.error("Error converting S3 object to String", e);
        } finally {
            IOUtils.closeQuietly(is, LOG);
        }
        return s3Body;
    }

    private void deleteMessageFromS3(PayloadS3Pointer s3Pointer) {
        try {
            amazonS3.deleteObject(s3Pointer.getS3BucketName(), s3Pointer.getS3Key());
            LOG.info("Message deleted from S3: " + s3Pointer.toJson());
        } catch (AmazonServiceException e) {
            LOG.error("A service exception", e);
        } catch (SdkClientException e) {
            LOG.error("Some sort of client exception", e);
        }
    }
}
