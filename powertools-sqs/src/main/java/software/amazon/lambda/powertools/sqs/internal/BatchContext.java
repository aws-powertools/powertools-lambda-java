package software.amazon.lambda.powertools.sqs.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;

import static com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public final class BatchContext {
    private static final Log LOG = LogFactory.getLog(BatchContext.class);

    private final List<SQSMessage> success = new ArrayList<>();
    private final List<SQSMessage> failures = new ArrayList<>();
    private final List<Exception> exceptions = new ArrayList<>();
    private final SqsClient client;

    public BatchContext(SqsClient client) {
        this.client = client;
    }

    public void addSuccess(SQSMessage event) {
        success.add(event);
    }

    public void addFailure(SQSMessage event, Exception e) {
        failures.add(event);
        exceptions.add(e);
    }

    public void processSuccessAndReset(final boolean suppressException) {
        try {
            if (hasFailures()) {

                deleteSuccessMessage();

                if (suppressException) {
                    List<String> messageIds = failures.stream().map(SQSMessage::getMessageId).collect(toList());
                    LOG.debug(format("[%s] records failed processing, but exceptions are suppressed. Failed messages %s", failures.size(), messageIds));
                } else {
                    throw new SQSBatchProcessingException(exceptions);
                }
            }
        } finally {
            reset();
        }
    }

    private boolean hasFailures() {
        return !failures.isEmpty();
    }

    private void deleteSuccessMessage() {
        if (!success.isEmpty()) {
            DeleteMessageBatchRequest request = DeleteMessageBatchRequest.builder()
                    .queueUrl(url())
                    .entries(success.stream().map(m -> DeleteMessageBatchRequestEntry.builder()
                            .id(m.getMessageId())
                            .receiptHandle(m.getReceiptHandle())
                            .build()).collect(toList()))
                    .build();

            client.deleteMessageBatch(request);
        }
    }

    private String url() {
        String[] arnArray = success.get(0).getEventSourceArn().split(":");
        return client.getQueueUrl(GetQueueUrlRequest.builder()
                .queueOwnerAWSAccountId(arnArray[1])
                .queueName(arnArray[2])
                .build())
                .queueUrl();
    }

    private void reset() {
        success.clear();
        failures.clear();
        exceptions.clear();
    }
}
