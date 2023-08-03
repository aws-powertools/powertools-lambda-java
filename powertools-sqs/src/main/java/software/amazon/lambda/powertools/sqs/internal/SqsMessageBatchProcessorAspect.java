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

package software.amazon.lambda.powertools.sqs.internal;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.sqs.SqsUtils.batchProcessor;
import static software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect.placedOnSqsEventRequestHandler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.sqs.SqsBatch;

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

            batchProcessor(sqsEvent,
                    sqsBatch.suppressException(),
                    sqsBatch.value(),
                    sqsBatch.deleteNonRetryableMessageFromQueue(),
                    sqsBatch.nonRetryableExceptions());
        }

        return pjp.proceed(proceedArgs);
    }
}
