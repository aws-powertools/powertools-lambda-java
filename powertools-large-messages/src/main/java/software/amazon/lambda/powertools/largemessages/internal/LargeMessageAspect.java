package software.amazon.lambda.powertools.largemessages.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.largemessages.LargeMessage;

import java.util.Optional;

import static java.lang.String.format;


@Aspect
public class LargeMessageAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LargeMessageAspect.class);

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(largeMessage)")
    public void callAt(LargeMessage largeMessage) {
    }

    @Around(value = "callAt(largeMessage) && execution(@LargeMessage * *.*(..))", argNames = "pjp,largeMessage")
    public Object around(ProceedingJoinPoint pjp,
                         LargeMessage largeMessage) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        // we need a message to process
        if (proceedArgs.length == 0) {
            LOG.warn("@LargeMessage annotation is placed on a method without any message to process, proceeding");
            return pjp.proceed(proceedArgs);
        }

        Object message = proceedArgs[0];
        Optional<LargeMessageProcessor<?>> largeMessageProcessor = LargeMessageProcessorFactory.get(message);

        if (!largeMessageProcessor.isPresent()) {
            LOG.warn(format("@LargeMessage annotation is placed on a method with unsupported message type [%s], proceeding", message.getClass()));
            return pjp.proceed(proceedArgs);
        }

        return largeMessageProcessor.get().process(pjp, largeMessage.deleteS3Objects());
    }

}
