package software.amazon.lambda.powertools.sqs.internal;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.sqs.SqsBatch;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.sqs.SqsUtils.batchProcessor;
import static software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect.placedOnSqsEventRequestHandler;

@Aspect
public class SqsMessageBatchProcessorAspect {

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(sqsBatch)")
    public void callAt(SqsBatch sqsBatch) {
    }

    @Around(value = "callAt(sqsBatch) && execution(@SqsBatch * *.*(..))", argNames = "pjp,sqsBatch")
    public Object around(ProceedingJoinPoint pjp,
                         SqsBatch sqsBatch) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)
                && placedOnSqsEventRequestHandler(pjp)) {

            SQSEvent sqsEvent = (SQSEvent) proceedArgs[0];

            batchProcessor(sqsEvent, sqsBatch.suppressException(), sqsBatch.value(), sqsBatch.nonRetryableExceptions());
        }

        return pjp.proceed(proceedArgs);
    }
}
