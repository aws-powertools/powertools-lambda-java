package software.amazon.lambda.powertools.sqs.exception;

/**
 * Indicates that a message has been skipped due to the batch it is
 * within failing.
 */
public class SkippedMessageDueToFailedBatchException extends Exception {

    public SkippedMessageDueToFailedBatchException() {
    }

}
