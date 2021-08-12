/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.sqs;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.sqs.internal.BatchContext;
import software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect;
import software.amazon.payloadoffloading.PayloadS3Pointer;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static software.amazon.lambda.powertools.sqs.internal.SqsLargeMessageAspect.processMessages;

/**
 * A class of helper functions to add additional functionality to {@link SQSEvent} processing.
 */
public final class SqsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SqsUtils.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static SqsClient client;

    private SqsUtils() {
    }

    /**
     * This is a utility method when you want to avoid using {@code SqsLargeMessage} annotation.
     * Gives you access to enriched messages from S3 in the SQS event produced via extended client lib.
     * If all the large S3 payload are successfully retrieved, it will delete them from S3 post success.
     *
     * @param sqsEvent        Event received from SQS Extended client library
     * @param messageFunction Function to execute you business logic which provides access to enriched messages from S3 when needed.
     * @return Return value from the function.
     */
    public static <R> R enrichedMessageFromS3(final SQSEvent sqsEvent,
                                              final Function<List<SQSMessage>, R> messageFunction) {
        return enrichedMessageFromS3(sqsEvent, true, messageFunction);
    }

    /**
     * This is a utility method when you want to avoid using {@code SqsLargeMessage} annotation.
     * Gives you access to enriched messages from S3 in the SQS event produced via extended client lib.
     * if all the large S3 payload are successfully retrieved, Control if it will delete payload from S3 post success.
     *
     * @param sqsEvent        Event received from SQS Extended client library
     * @param messageFunction Function to execute you business logic which provides access to enriched messages from S3 when needed.
     * @return Return value from the function.
     */
    public static <R> R enrichedMessageFromS3(final SQSEvent sqsEvent,
                                              final boolean deleteS3Payload,
                                              final Function<List<SQSMessage>, R> messageFunction) {

        List<SQSMessage> sqsMessages = sqsEvent.getRecords().stream()
                .map(SqsUtils::clonedMessage)
                .collect(Collectors.toList());

        List<PayloadS3Pointer> s3Pointers = processMessages(sqsMessages);

        R returnValue = messageFunction.apply(sqsMessages);

        if (deleteS3Payload) {
            s3Pointers.forEach(SqsLargeMessageAspect::deleteMessage);
        }

        return returnValue;
    }

    /**
     * Provides ability to set default {@link SqsClient} to be used by utility.
     * If no default configuration is provided, client is instantiated via {@link SqsClient#create()}
     *
     * @param client {@link SqsClient} to be used by utility
     */
    public static void overrideSqsClient(SqsClient client) {
        SqsUtils.client = client;
    }

    /**
     * This utility method is used to processes each {@link SQSMessage} inside received {@link SQSEvent}
     *
     * <p>
     * Utility will take care of calling {@link SqsMessageHandler#process(SQSMessage)} method for each {@link SQSMessage}
     * in the received {@link SQSEvent}
     * </p>
     *
     * </p>
     * If any exception is thrown from {@link SqsMessageHandler#process(SQSMessage)} during processing of a messages,
     * Utility will take care of deleting all the successful messages from SQS. When one or more single message fails
     * processing due to exception thrown from {@link SqsMessageHandler#process(SQSMessage)}
     * {@link SQSBatchProcessingException} is thrown with all the details of successful and failed messages.
     * <p>
     * If all the messages are successfully processes, No SQS messages are deleted explicitly but is rather delegated to
     * Lambda execution context for deletion.
     * </p>
     *
     * <p>
     * If you dont want to utility to throw {@link SQSBatchProcessingException} in case of failures but rather suppress
     * it, Refer {@link SqsUtils#batchProcessor(SQSEvent, boolean, Class)}
     * </p>
     *
     * @param event   {@link SQSEvent} received by lambda function.
     * @param handler Class implementing {@link SqsMessageHandler} which will be called for each message in event.
     * @return List of values returned by {@link SqsMessageHandler#process(SQSMessage)} while processing each message.
     * @throws SQSBatchProcessingException if some messages fail during processing.
     */
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final Class<? extends SqsMessageHandler<R>> handler) {
        return batchProcessor(event, false, handler);
    }

    @SafeVarargs
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final Class<? extends SqsMessageHandler<R>> handler,
                                             final Class<? extends Exception>... nonRetryableExceptions) {
        return batchProcessor(event, false, handler, nonRetryableExceptions);
    }

    /**
     * This utility method is used to processes each {@link SQSMessage} inside received {@link SQSEvent}
     *
     * <p>
     * Utility will take care of calling {@link SqsMessageHandler#process(SQSMessage)} method for each {@link SQSMessage}
     * in the received {@link SQSEvent}
     * </p>
     *
     * </p>
     * If any exception is thrown from {@link SqsMessageHandler#process(SQSMessage)} during processing of a messages,
     * Utility will take care of deleting all the successful messages from SQS. When one or more single message fails
     * processing due to exception thrown from {@link SqsMessageHandler#process(SQSMessage)}
     * {@link SQSBatchProcessingException} is thrown with all the details of successful and failed messages.
     * <p>
     * Exception can also be suppressed if desired.
     * <p>
     * If all the messages are successfully processes, No SQS messages are deleted explicitly but is rather delegated to
     * Lambda execution context for deletion.
     * </p>
     *
     * @param event             {@link SQSEvent} received by lambda function.
     * @param suppressException if this is set to true, No {@link SQSBatchProcessingException} is thrown even on failed
     *                          messages.
     * @param handler           Class implementing {@link SqsMessageHandler} which will be called for each message in event.
     * @return List of values returned by {@link SqsMessageHandler#process(SQSMessage)} while processing each message.
     * @throws SQSBatchProcessingException if some messages fail during processing and no suppression enabled.
     */
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final boolean suppressException,
                                             final Class<? extends SqsMessageHandler<R>> handler) {

        SqsMessageHandler<R> handlerInstance = instantiatedHandler(handler);
        return batchProcessor(event, suppressException, handlerInstance);
    }

    @SafeVarargs
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final boolean suppressException,
                                             final Class<? extends SqsMessageHandler<R>> handler,
                                             final Class<? extends Exception>... nonRetryableExceptions) {

        SqsMessageHandler<R> handlerInstance = instantiatedHandler(handler);
        return batchProcessor(event, suppressException, handlerInstance, false, nonRetryableExceptions);
    }

    @SafeVarargs
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final boolean suppressException,
                                             final Class<? extends SqsMessageHandler<R>> handler,
                                             final boolean deleteNonRetryableMessageFromQueue,
                                             final Class<? extends Exception>... nonRetryableExceptions) {

        SqsMessageHandler<R> handlerInstance = instantiatedHandler(handler);
        return batchProcessor(event, suppressException, handlerInstance, deleteNonRetryableMessageFromQueue, nonRetryableExceptions);
    }

    /**
     * This utility method is used to processes each {@link SQSMessage} inside received {@link SQSEvent}
     *
     * <p>
     * Utility will take care of calling {@link SqsMessageHandler#process(SQSMessage)} method for each {@link SQSMessage}
     * in the received {@link SQSEvent}
     * </p>
     *
     * </p>
     * If any exception is thrown from {@link SqsMessageHandler#process(SQSMessage)} during processing of a messages,
     * Utility will take care of deleting all the successful messages from SQS. When one or more single message fails
     * processing due to exception thrown from {@link SqsMessageHandler#process(SQSMessage)}
     * {@link SQSBatchProcessingException} is thrown with all the details of successful and failed messages.
     * <p>
     * If all the messages are successfully processes, No SQS messages are deleted explicitly but is rather delegated to
     * Lambda execution context for deletion.
     * </p>
     *
     * <p>
     * If you dont want to utility to throw {@link SQSBatchProcessingException} in case of failures but rather suppress
     * it, Refer {@link SqsUtils#batchProcessor(SQSEvent, boolean, SqsMessageHandler)}
     * </p>
     *
     * @param event   {@link SQSEvent} received by lambda function.
     * @param handler Instance of class implementing {@link SqsMessageHandler} which will be called for each message in event.
     * @return List of values returned by {@link SqsMessageHandler#process(SQSMessage)} while processing each message-
     * @throws SQSBatchProcessingException if some messages fail during processing.
     */
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final SqsMessageHandler<R> handler) {
        return batchProcessor(event, false, handler);
    }

    @SafeVarargs
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final SqsMessageHandler<R> handler,
                                             final Class<? extends Exception>... nonRetryableExceptions) {
        return batchProcessor(event, false, handler, false, nonRetryableExceptions);
    }


    /**
     * This utility method is used to processes each {@link SQSMessage} inside received {@link SQSEvent}
     *
     * <p>
     * Utility will take care of calling {@link SqsMessageHandler#process(SQSMessage)} method for each {@link SQSMessage}
     * in the received {@link SQSEvent}
     * </p>
     *
     * </p>
     * If any exception is thrown from {@link SqsMessageHandler#process(SQSMessage)} during processing of a messages,
     * Utility will take care of deleting all the successful messages from SQS. When one or more single message fails
     * processing due to exception thrown from {@link SqsMessageHandler#process(SQSMessage)}
     * {@link SQSBatchProcessingException} is thrown with all the details of successful and failed messages.
     * <p>
     * Exception can also be suppressed if desired.
     * <p>
     * If all the messages are successfully processes, No SQS messages are deleted explicitly but is rather delegated to
     * Lambda execution context for deletion.
     * </p>
     *
     * @param event             {@link SQSEvent} received by lambda function.
     * @param suppressException if this is set to true, No {@link SQSBatchProcessingException} is thrown even on failed
     *                          messages.
     * @param handler           Instance of class implementing {@link SqsMessageHandler} which will be called for each message in event.
     * @return List of values returned by {@link SqsMessageHandler#process(SQSMessage)} while processing each message.
     * @throws SQSBatchProcessingException if some messages fail during processing and no suppression enabled.
     */
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final boolean suppressException,
                                             final SqsMessageHandler<R> handler) {
        return batchProcessor(event, suppressException, handler, false);

    }

    @SafeVarargs
    public static <R> List<R> batchProcessor(final SQSEvent event,
                                             final boolean suppressException,
                                             final SqsMessageHandler<R> handler,
                                             final boolean deleteNonRetryableMessageFromQueue,
                                             final Class<? extends Exception>... nonRetryableExceptions) {
        final List<R> handlerReturn = new ArrayList<>();

        if(client == null) {
            client = SqsClient.create();
        }

        BatchContext batchContext = new BatchContext(client);

        for (SQSMessage message : event.getRecords()) {
            try {
                handlerReturn.add(handler.process(message));
                batchContext.addSuccess(message);
            } catch (Exception e) {
                batchContext.addFailure(message, e);
            }
        }

        batchContext.processSuccessAndHandleFailed(handlerReturn, suppressException, deleteNonRetryableMessageFromQueue, nonRetryableExceptions);

        return handlerReturn;
    }

    private static <R> SqsMessageHandler<R> instantiatedHandler(final Class<? extends SqsMessageHandler<R>> handler) {

        try {
            if (null == handler.getDeclaringClass()) {
                return handler.getDeclaredConstructor().newInstance();
            }

            final Constructor<? extends SqsMessageHandler<R>> constructor = handler.getDeclaredConstructor(handler.getDeclaringClass());
            constructor.setAccessible(true);
            return constructor.newInstance(handler.getDeclaringClass().getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            LOG.error("Failed creating handler instance", e);
            throw new RuntimeException("Unexpected error occurred. Please raise issue at " +
                    "https://github.com/awslabs/aws-lambda-powertools-java/issues", e);
        }
    }

    private static SQSMessage clonedMessage(final SQSMessage sqsMessage) {
        try {
            return objectMapper
                    .readValue(objectMapper.writeValueAsString(sqsMessage), SQSMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectMapper objectMapper() {
        return objectMapper;
    }
}
