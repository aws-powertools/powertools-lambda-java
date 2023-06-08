package software.amazon.lambda.powertools.sqs.exception;

public class SkippedMessageDueToFailedBatchException extends Exception {
    private final String messageGroupId;

    public SkippedMessageDueToFailedBatchException(String messageGroupId) {
        this.messageGroupId = messageGroupId;
    }

    public String getMessageGroupId() {
        return messageGroupId;
    }
}
