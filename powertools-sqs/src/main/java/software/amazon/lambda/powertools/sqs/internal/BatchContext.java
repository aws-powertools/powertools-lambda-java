package software.amazon.lambda.powertools.sqs.internal;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.lambda.powertools.sqs.SQSBatchProcessingException;

import static java.util.stream.Collectors.toList;

public final class BatchContext {
    private List<SQSEvent.SQSMessage> success = new ArrayList<>();
    private List<SQSEvent.SQSMessage> failures = new ArrayList<>();
    private List<Exception> exceptions = new ArrayList<>();
    private SqsClient client;

    public BatchContext(SqsClient client) {
        this.client = client;
    }

    public void addSuccess(SQSEvent.SQSMessage event) {
        success.add(event);
    }

    public void addFailure(SQSEvent.SQSMessage event, Exception e) {
        failures.add(event);
        exceptions.add(e);
    }

    private boolean hasFailures() {
        return !failures.isEmpty();
    }

    private void cleanUpAndReset() {
        DeleteMessageBatchRequest request = DeleteMessageBatchRequest.builder()
                .queueUrl(url())
                .entries(success.stream().map(m -> DeleteMessageBatchRequestEntry.builder()
                        .id(m.getMessageId())
                        .receiptHandle(m.getReceiptHandle())
                        .build()).collect(toList()))
                .build();

        client.deleteMessageBatch(request);
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
        success = new ArrayList<>();
        failures = new ArrayList<>();
        exceptions = new ArrayList<>();
    }

    public void processSuccessAndReset(final boolean suppressException) {
        if (hasFailures() && !suppressException) {
            SQSBatchProcessingException exception = new SQSBatchProcessingException(exceptions);
            cleanUpAndReset();
            throw exception;
        } else if (hasFailures()) {
            // LOG
            cleanUpAndReset();
        } else {
            reset();
        }
    }
}
