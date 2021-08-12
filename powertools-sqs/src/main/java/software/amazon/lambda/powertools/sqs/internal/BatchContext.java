package software.amazon.lambda.powertools.sqs.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;
import software.amazon.lambda.powertools.sqs.SqsUtils;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public final class BatchContext {
    private static final Logger LOG = LoggerFactory.getLogger(BatchContext.class);
    private static final Map<String, String> QUEUE_ARN_TO_DLQ_URL_MAPPING = new HashMap<>();

    private final Map<SQSMessage, Exception> messageToException = new HashMap<>();
    private final List<SQSMessage> success = new ArrayList<>();

    private final SqsClient client;

    public BatchContext(SqsClient client) {
        this.client = client;
    }

    public void addSuccess(SQSMessage event) {
        success.add(event);
    }

    public void addFailure(SQSMessage event, Exception e) {
        messageToException.put(event, e);
    }

    @SafeVarargs
    public final <T> void processSuccessAndHandleFailed(final List<T> successReturns,
                                                        final boolean suppressException,
                                                        final boolean deleteNonRetryableMessageFromQueue,
                                                        final Class<? extends Exception>... nonRetryableExceptions) {
        if (hasFailures()) {

            List<Exception> exceptions = new ArrayList<>();
            List<SQSMessage> failedMessages = new ArrayList<>();
            Map<SQSMessage, Exception> nonRetryableMessageToException = new HashMap<>();

            if (nonRetryableExceptions.length == 0) {
                exceptions.addAll(messageToException.values());
                failedMessages.addAll(messageToException.keySet());
            } else {
                messageToException.forEach((sqsMessage, exception) -> {
                    boolean nonRetryableException = isNonRetryableException(exception, nonRetryableExceptions);

                    if (nonRetryableException) {
                        nonRetryableMessageToException.put(sqsMessage, exception);
                    } else {
                        exceptions.add(exception);
                        failedMessages.add(sqsMessage);
                    }
                });
            }

            List<SQSMessage> messagesToBeDeleted = new ArrayList<>(success);

            if (!nonRetryableMessageToException.isEmpty() && deleteNonRetryableMessageFromQueue) {
                messagesToBeDeleted.addAll(nonRetryableMessageToException.keySet());
            } else if (!nonRetryableMessageToException.isEmpty()) {

                boolean isMovedToDlq = moveNonRetryableMessagesToDlqIfConfigured(nonRetryableMessageToException);

                if (!isMovedToDlq) {
                    exceptions.addAll(nonRetryableMessageToException.values());
                    failedMessages.addAll(nonRetryableMessageToException.keySet());
                }
            }

            deleteMessagesFromQueue(messagesToBeDeleted);

            processFailedMessages(successReturns, suppressException, exceptions, failedMessages);
        }
    }

    private <T> void processFailedMessages(List<T> successReturns,
                                           boolean suppressException,
                                           List<Exception> exceptions,
                                           List<SQSMessage> failedMessages) {
        if (failedMessages.isEmpty()) {
            return;
        }

        if (suppressException) {
            List<String> messageIds = failedMessages.stream().
                    map(SQSMessage::getMessageId)
                    .collect(toList());

            LOG.debug(format("[%s] records failed processing, but exceptions are suppressed. " +
                    "Failed messages %s", failedMessages.size(), messageIds));
        } else {
            throw new SQSBatchProcessingException(exceptions, failedMessages, successReturns);
        }
    }

    private boolean isNonRetryableException(Exception exception, Class<? extends Exception>[] nonRetryableExceptions) {
        return Arrays.stream(nonRetryableExceptions)
                .anyMatch(aClass -> aClass.isInstance(exception));
    }

    private boolean moveNonRetryableMessagesToDlqIfConfigured(Map<SQSMessage, Exception> nonRetryableMessageToException) {
        Optional<String> dlqUrl = fetchDlqUrl(nonRetryableMessageToException);

        if (!dlqUrl.isPresent()) {
            return false;
        }

        List<SendMessageBatchRequestEntry> dlqMessages = nonRetryableMessageToException.keySet().stream()
                .map(sqsMessage -> {
                    Map<String, MessageAttributeValue> messageAttributesMap = new HashMap<>();

                    sqsMessage.getMessageAttributes().forEach((s, messageAttribute) -> {
                        MessageAttributeValue.Builder builder = MessageAttributeValue.builder();

                        builder
                                .dataType(messageAttribute.getDataType())
                                .stringValue(messageAttribute.getStringValue());

                        if (null != messageAttribute.getBinaryValue()) {
                            builder.binaryValue(SdkBytes.fromByteBuffer(messageAttribute.getBinaryValue()));
                        }

                        messageAttributesMap.put(s, builder.build());
                    });

                    return SendMessageBatchRequestEntry.builder()
                            .messageBody(sqsMessage.getBody())
                            .id(sqsMessage.getMessageId())
                            .messageAttributes(messageAttributesMap)
                            .build();
                })
                .collect(toList());

        SendMessageBatchResponse sendMessageBatchResponse = client.sendMessageBatch(builder -> builder.queueUrl(dlqUrl.get())
                .entries(dlqMessages));

        LOG.debug("Response from send batch message to DLQ request {}", sendMessageBatchResponse);

        return true;
    }

    private Optional<String> fetchDlqUrl(Map<SQSMessage, Exception> nonRetryableMessageToException) {
        return nonRetryableMessageToException.keySet().stream()
                .findFirst()
                .map(sqsMessage -> QUEUE_ARN_TO_DLQ_URL_MAPPING.computeIfAbsent(sqsMessage.getEventSourceArn(), sourceArn -> {
                    String queueUrl = url(sourceArn);

                    GetQueueAttributesResponse queueAttributes = client.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .attributeNames(QueueAttributeName.REDRIVE_POLICY)
                            .queueUrl(queueUrl)
                            .build());

                    return ofNullable(queueAttributes.attributes().get(QueueAttributeName.REDRIVE_POLICY))
                            .map(policy -> {
                                try {
                                    return SqsUtils.objectMapper().readTree(policy);
                                } catch (JsonProcessingException e) {
                                    LOG.debug("Unable to parse Re drive policy for queue {}. Even if DLQ exists, failed messages will be send back to main queue.", queueUrl, e);
                                    return null;
                                }
                            })
                            .map(node -> node.get("deadLetterTargetArn"))
                            .map(JsonNode::asText)
                            .map(this::url)
                            .orElse(null);
                }));
    }

    private boolean hasFailures() {
        return !messageToException.isEmpty();
    }

    private void deleteMessagesFromQueue(final List<SQSMessage> messages) {
        if (!messages.isEmpty()) {
            DeleteMessageBatchRequest request = DeleteMessageBatchRequest.builder()
                    .queueUrl(url(messages.get(0).getEventSourceArn()))
                    .entries(messages.stream().map(m -> DeleteMessageBatchRequestEntry.builder()
                            .id(m.getMessageId())
                            .receiptHandle(m.getReceiptHandle())
                            .build()).collect(toList()))
                    .build();

            DeleteMessageBatchResponse deleteMessageBatchResponse = client.deleteMessageBatch(request);
            LOG.debug("Response from delete request {}", deleteMessageBatchResponse);
        }
    }

    private String url(String queueArn) {
        String[] arnArray = queueArn.split(":");
        return String.format("https://sqs.%s.amazonaws.com/%s/%s", arnArray[3], arnArray[4], arnArray[5]);
    }
}
