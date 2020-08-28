package software.amazon.lambda.powertools.sqs.internal;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
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
            List<PayloadS3Pointer> payloadS3Pointers = collectPayloadPointers(proceedArgs);

            Object[] rewrittenArgs = rewriteMessages(proceedArgs);
            Object proceed = pjp.proceed(rewrittenArgs);

            payloadS3Pointers.forEach(this::deleteMessageFromS3);
            return proceed;
        }

        return pjp.proceed(proceedArgs);
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

        for (SQSMessage sqsMessage : sqsEvent.getRecords()) {
            if (isBodyLargeMessagePointer(sqsMessage.getBody())) {
                PayloadS3Pointer s3Pointer = PayloadS3Pointer.fromJson(sqsMessage.getBody());

                Optional<S3Object> s3Object = callS3Gracefully(s3Pointer, pointer -> {
                    S3Object object = amazonS3.getObject(pointer.getS3BucketName(), pointer.getS3Key());
                    LOG.debug("Object downloaded with key: " + s3Pointer.getS3Key());
                    return object;
                });

                s3Object.flatMap(this::readStringFromS3Object)
                        .ifPresent(sqsMessage::setBody);
            }
        }

        args[0] = sqsEvent;

        return args;
    }

    private boolean isBodyLargeMessagePointer(String record) {
        return record.startsWith("[\"software.amazon.payloadoffloading.PayloadS3Pointer\"");
    }

    private Optional<String> readStringFromS3Object(S3Object object) {
        try (S3ObjectInputStream is = object.getObjectContent()) {
            return of(IOUtils.toString(is));
        } catch (IOException e) {
            LOG.error("Error converting S3 object to String", e);
        }

        return empty();
    }

    private void deleteMessageFromS3(PayloadS3Pointer s3Pointer) {
        callS3Gracefully(s3Pointer, pointer -> {
            amazonS3.deleteObject(s3Pointer.getS3BucketName(), s3Pointer.getS3Key());
            LOG.info("Message deleted from S3: " + s3Pointer.toJson());
            return null;
        });
    }

    private <R> Optional<R> callS3Gracefully(final PayloadS3Pointer pointer,
                                             final Function<PayloadS3Pointer, R> function) {
        try {
            return ofNullable(function.apply(pointer));
        } catch (AmazonServiceException e) {
            LOG.error("A service exception", e);
        } catch (SdkClientException e) {
            LOG.error("Some sort of client exception", e);
        }

        return empty();
    }

    public static boolean placedOnSqsEventRequestHandler(ProceedingJoinPoint pjp) {
        return pjp.getArgs().length == 2
                && pjp.getArgs()[0] instanceof SQSEvent
                && pjp.getArgs()[1] instanceof Context;
    }
}
