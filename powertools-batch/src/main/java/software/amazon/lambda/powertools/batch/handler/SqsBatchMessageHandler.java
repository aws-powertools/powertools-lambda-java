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

package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.utilities.EventDeserializer;

/**
 * A batch message processor for SQS batches.
 *
 * @param <M> The user-defined type of the message payload
 * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#services-sqs-batchfailurereporting">SQS Batch failure reporting</a>
 */
public class SqsBatchMessageHandler<M> implements BatchMessageHandler<SQSEvent, SQSBatchResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqsBatchMessageHandler.class);

    // The attribute on an SQS-FIFO message used to record the message group ID
    // https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#sample-fifo-queues-message-event
    private static final String MESSAGE_GROUP_ID_KEY = "MessageGroupId";

    private final Class<M> messageClass;
    private final BiConsumer<M, Context> messageHandler;
    private final BiConsumer<SQSEvent.SQSMessage, Context> rawMessageHandler;
    private final Consumer<SQSEvent.SQSMessage> successHandler;
    private final BiConsumer<SQSEvent.SQSMessage, Throwable> failureHandler;

    public SqsBatchMessageHandler(BiConsumer<M, Context> messageHandler, Class<M> messageClass,
                                  BiConsumer<SQSEvent.SQSMessage, Context> rawMessageHandler,
                                  Consumer<SQSEvent.SQSMessage> successHandler,
                                  BiConsumer<SQSEvent.SQSMessage, Throwable> failureHandler) {
        this.messageHandler = messageHandler;
        this.messageClass = messageClass;
        this.rawMessageHandler = rawMessageHandler;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public SQSBatchResponse processBatch(SQSEvent event, Context context) {
        SQSBatchResponse response = SQSBatchResponse.builder().withBatchItemFailures(new ArrayList<>()).build();

        // If we are working on a FIFO queue, when any message fails we should stop processing and return the
        // rest of the batch as failed too. We use this variable to track when that has happened.
        // https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#services-sqs-batchfailurereporting
        boolean failWholeBatch = false;

        int messageCursor = 0;
        for (; messageCursor < event.getRecords().size() && !failWholeBatch; messageCursor++) {
            SQSEvent.SQSMessage message = event.getRecords().get(messageCursor);

            String messageGroupId = message.getAttributes() != null ?
                    message.getAttributes().get(MESSAGE_GROUP_ID_KEY) : null;

            try {
                if (this.rawMessageHandler != null) {
                    rawMessageHandler.accept(message, context);
                } else {
                    M messageDeserialized = EventDeserializer.extractDataFrom(message).as(messageClass);
                    messageHandler.accept(messageDeserialized, context);
                }

                // Report success if we have a handler
                if (this.successHandler != null) {
                    this.successHandler.accept(message);
                }

            } catch (Throwable t) {
                LOGGER.error("Error while processing message with messageId {}: {}, adding it to batch item failures",
                        message.getMessageId(), t.getMessage());
                LOGGER.error("Error was", t);

                response.getBatchItemFailures()
                        .add(SQSBatchResponse.BatchItemFailure.builder().withItemIdentifier(message.getMessageId())
                                .build());
                if (messageGroupId != null) {
                    failWholeBatch = true;
                    LOGGER.info(
                            "A message in a batch with messageGroupId {} and messageId {} failed; failing the rest of the batch too"
                            , messageGroupId, message.getMessageId());
                }

                // Report failure if we have a handler
                if (this.failureHandler != null) {
                    // A failing failure handler is no reason to fail the batch
                    try {
                        this.failureHandler.accept(message, t);
                    } catch (Throwable t2) {
                        LOGGER.warn("failureHandler threw handling failure", t2);
                    }
                }

            }
        }

        if (failWholeBatch) {
            // Add the remaining messages to the batch item failures
            event.getRecords()
                    .subList(messageCursor, event.getRecords().size())
                    .forEach(message -> response.getBatchItemFailures()
                            .add(SQSBatchResponse.BatchItemFailure.builder().withItemIdentifier(message.getMessageId())
                                    .build()));
        }
        return response;
    }
}
