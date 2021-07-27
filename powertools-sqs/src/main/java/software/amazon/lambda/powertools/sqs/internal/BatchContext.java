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
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;
import software.amazon.lambda.powertools.sqs.SqsUtils;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public final class BatchContext {
    private static final Logger LOG = LoggerFactory.getLogger(BatchContext.class);
    private static final Map<String, String> queueArnToQueueUrlMapping = new HashMap<>();
    private static final Map<String, String> queueArnToDlqUrlMapping = new HashMap<>();

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

            messageToException.forEach((sqsMessage, exception) -> {
                boolean nonRetryableMessage = Arrays.stream(nonRetryableExceptions)
                        .anyMatch(aClass -> aClass.isInstance(exception));

                if (nonRetryableMessage) {
                    nonRetryableMessageToException.put(sqsMessage, exception);
                } else {
                    exceptions.add(exception);
                    failedMessages.add(sqsMessage);
                }
            });

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

        LOG.debug(format("Response from send batch message to DLQ request %s", sendMessageBatchResponse));

        return true;
    }

    private Optional<String> fetchDlqUrl(Map<SQSMessage, Exception> nonRetryableMessageToException) {
        return nonRetryableMessageToException.keySet().stream()
                .findFirst()
                .map(sqsMessage -> queueArnToDlqUrlMapping.computeIfAbsent(sqsMessage.getEventSourceArn(), sourceArn -> {
                    String queueUrl = url(sourceArn);

                    GetQueueAttributesResponse queueAttributes = client.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .attributeNames(QueueAttributeName.REDRIVE_POLICY)
                            .queueUrl(queueUrl)
                            .build());

                    try {
                        JsonNode jsonNode = SqsUtils.objectMapper().readTree(queueAttributes.attributes().get(QueueAttributeName.REDRIVE_POLICY));
                        return url(jsonNode.get("deadLetterTargetArn").asText());
                    } catch (JsonProcessingException e) {
                        LOG.debug("Unable to parse Re drive policy for queue {}. Even if DLQ exists, failed messages will be send back to main queue.", queueUrl, e);
                        return null;
                    }
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
            LOG.debug(format("Response from delete request %s", deleteMessageBatchResponse));
        }
    }

    private String url(String queueArn) {
        return queueArnToQueueUrlMapping.computeIfAbsent(queueArn, s -> {
            String[] arnArray = queueArn.split(":");

            return client.getQueueUrl(GetQueueUrlRequest.builder()
                            .queueOwnerAWSAccountId(arnArray[4])
                            .queueName(arnArray[5])
                            .build())
                    .queueUrl();
        });
    }
}
