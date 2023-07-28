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

package software.amazon.lambda.powertools.sqs.handlers;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static software.amazon.lambda.powertools.sqs.internal.SqsMessageBatchProcessorAspectTest.interactionClient;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.sqs.SqsBatch;
import software.amazon.lambda.powertools.sqs.SqsMessageHandler;

public class SqsMessageHandlerWithNonRetryableHandlerWithDelete implements RequestHandler<SQSEvent, String> {

    @Override
    @SqsBatch(value = InnerMessageHandler.class,
            nonRetryableExceptions = {IllegalStateException.class, IllegalArgumentException.class},
            deleteNonRetryableMessageFromQueue = true)
    public String handleRequest(final SQSEvent sqsEvent,
                                final Context context) {
        return "Success";
    }

    private class InnerMessageHandler implements SqsMessageHandler<Object> {

        @Override
        public String process(SQSMessage message) {
            if (message.getMessageId().isEmpty()) {
                throw new IllegalArgumentException("Invalid message and was moved to DLQ");
            }

            if ("2e1424d4-f796-459a-9696-9c92662ba5da".equals(message.getMessageId())) {
                throw new RuntimeException("Invalid message and should be reprocessed");
            }

            interactionClient.listQueues();
            return "Success";
        }
    }
}
