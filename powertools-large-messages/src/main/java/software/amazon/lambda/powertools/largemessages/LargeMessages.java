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

package software.amazon.lambda.powertools.largemessages;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.lambda.powertools.largemessages.internal.LargeMessageProcessor;
import software.amazon.lambda.powertools.largemessages.internal.LargeMessageProcessorFactory;

/**
 * Functional API for processing large messages without AspectJ.
 * <p>
 * Use this class to handle large messages (> 1 MB) from SQS or SNS.
 * When large messages are sent to an SQS Queue or SNS Topic, they are offloaded to S3 and only a reference is passed in the message/record.
 * <p>
 * {@code LargeMessages} automatically retrieves and optionally deletes messages
 * which have been offloaded to S3 using the {@code amazon-sqs-java-extended-client-lib} or {@code amazon-sns-java-extended-client-lib}
 * client libraries.
 * <p>
 * This version is compatible with version 1.1.0+ and 2.0.0+ of {@code amazon-sqs-java-extended-client-lib} / {@code amazon-sns-java-extended-client-lib}.
 * <p>
 * <u>SQS Example</u>:
 * <pre>
 * public class SqsBatchHandler implements RequestHandler&lt;SQSEvent, SQSBatchResponse&gt; {
 *     private final BatchMessageHandler&lt;SQSEvent, SQSBatchResponse&gt; handler;
 *
 *     public SqsBatchHandler() {
 *         handler = new BatchMessageHandlerBuilder()
 *                 .withSqsBatchHandler()
 *                 .buildWithRawMessageHandler(this::processMessage);
 *     }
 *
 *     &#64;Override
 *     public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
 *         return handler.processBatch(sqsEvent, context);
 *     }
 *
 *     private void processMessage(SQSEvent.SQSMessage sqsMessage) {
 *         LargeMessages.processLargeMessage(sqsMessage, this::handleProcessedMessage);
 *     }
 *
 *     private void handleProcessedMessage(SQSEvent.SQSMessage processedMessage) {
 *         // processedMessage.getBody() will contain the content of the S3 Object
 *     }
 * }
 * </pre>
 * <p>
 * To disable the deletion of S3 objects:
 * <pre>
 * LargeMessages.processLargeMessage(sqsMessage, this::handleProcessedMessage, false);
 * </pre>
 * <p>
 * For multi-argument methods, use a lambda to pass additional parameters:
 * <pre>
 * public void handleRequest(SQSEvent event, Context context) {
 *     event.getRecords().forEach(message -&gt;
 *         LargeMessages.processLargeMessage(message, processedMsg -&gt; processMessage(processedMsg, context))
 *     );
 * }
 *
 * private void processMessage(SQSMessage processedMessage, Context context) {
 *     // processedMessage.getBody() will contain the content of the S3 Object
 * }
 * </pre>
 * <p>
 * <b>Note 1</b>: The message object (SQSMessage or SNSRecord) is modified in-place to avoid duplicating
 * the large blob in memory. The message body will be replaced with the S3 object content.
 * <p>
 * <b>Note 2</b>: Retrieving payloads and deleting objects from S3 will increase the duration of the Lambda function.
 * <p>
 * <b>Note 3</b>: Make sure to configure your function with enough memory to be able to retrieve S3 objects.
 *
 * @see LargeMessage
 */
public final class LargeMessages {

    private static final Logger LOG = LoggerFactory.getLogger(LargeMessages.class);

    private LargeMessages() {
        // utility class
    }

    /**
     * Process a large message and execute the function with the processed message.
     * <p>
     * The S3 object will be deleted after processing (default behavior).
     * To disable S3 object deletion, use {@link #processLargeMessage(Object, Function, boolean)}.
     * <p>
     * Example usage:
     * <pre>
     * String returnValueOfFunction = LargeMessages.processLargeMessage(sqsMessage, this::handleMessage);
     * String returnValueOfFunction = LargeMessages.processLargeMessage(sqsMessage, processedMsg -&gt; processOrder(processedMsg, orderId, amount));
     * </pre>
     *
     * @param message the message to process (SQSMessage or SNSRecord)
     * @param function the function to execute with the processed message
     * @param <T> the message type
     * @param <R> the return type of the function
     * @return the result of the function execution
     */
    public static <T, R> R processLargeMessage(T message, Function<T, R> function) {
        return processLargeMessage(message, function, true);
    }

    /**
     * Process a large message and execute the function with the processed message.
     * <p>
     * Example usage:
     * <pre>
     * String returnValueOfFunction = LargeMessages.processLargeMessage(sqsMessage, this::handleMessage, false);
     * String returnValueOfFunction = LargeMessages.processLargeMessage(sqsMessage, processedMsg -&gt; processOrder(processedMsg, orderId, amount), false);
     * </pre>
     *
     * @param message the message to process (SQSMessage or SNSRecord)
     * @param function the function to execute with the processed message
     * @param deleteS3Object whether to delete the S3 object after processing
     * @param <T> the message type
     * @param <R> the return type of the function
     * @return the result of the function execution
     */
    public static <T, R> R processLargeMessage(T message, Function<T, R> function, boolean deleteS3Object) {
        Optional<LargeMessageProcessor<?>> processor = LargeMessageProcessorFactory.get(message);

        if (!processor.isPresent()) {
            LOG.warn("Unsupported message type [{}], proceeding without large message processing",
                    message.getClass());
            return function.apply(message);
        }

        try {
            @SuppressWarnings("unchecked")
            LargeMessageProcessor<T> typedProcessor = (LargeMessageProcessor<T>) processor.get();
            return typedProcessor.process(message, function::apply, deleteS3Object);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new LargeMessageProcessingException("Failed to process large message", t);
        }
    }
}
