package software.amazon.lambda.powertools.sqs.internal;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.sqs.SqsBatchProcessor;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.sqs.PowertoolsSqs.batchProcessor;
import static software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect.placedOnSqsEventRequestHandler;

@Aspect
public class SqsMessageBatchProcessorAspect {

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(sqsBatchProcessor)")
    public void callAt(SqsBatchProcessor sqsBatchProcessor) {
    }

    @Around(value = "callAt(sqsBatchProcessor) && execution(@SqsBatchProcessor * *.*(..))", argNames = "pjp,sqsBatchProcessor")
    public Object around(ProceedingJoinPoint pjp,
                         SqsBatchProcessor sqsBatchProcessor) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp)
                && placedOnSqsEventRequestHandler(pjp)) {

            SQSEvent sqsEvent = (SQSEvent) proceedArgs[0];

            batchProcessor(sqsEvent, sqsBatchProcessor.suppressException(), sqsBatchProcessor.value());
        }

        return pjp.proceed(proceedArgs);
    }
}
