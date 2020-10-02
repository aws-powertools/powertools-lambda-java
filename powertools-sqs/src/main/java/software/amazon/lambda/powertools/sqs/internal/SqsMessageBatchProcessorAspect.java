package software.amazon.lambda.powertools.sqs.internal;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.sqs.PowertoolsSqs;
import software.amazon.lambda.powertools.sqs.SqsBatchProcessor;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.sqs.internal.SqsMessageAspect.placedOnSqsEventRequestHandler;

@Aspect
public class SqsMessageBatchProcessorAspect {
    private static final SqsClient client = SqsClient.create();
    private static BatchContext details = new BatchContext(PowertoolsSqs.defaultSqsClient());

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

            PowertoolsSqs.partialBatchProcessor(sqsEvent, sqsBatchProcessor.suppressException(), sqsBatchProcessor.value().newInstance());
        }

        return pjp.proceed(proceedArgs);
    }
}
