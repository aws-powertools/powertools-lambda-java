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

import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.largemessages.LargeMessage;

/**
 * Handle {@link LargeMessage} annotations.
 */
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
            LOG.warn("@LargeMessage annotation is placed on a method with unsupported message type [{}], proceeding",
                    message.getClass());
            return pjp.proceed(proceedArgs);
        }

        return largeMessageProcessor.get().process(pjp, largeMessage.deleteS3Object());
    }

}
