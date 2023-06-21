package software.amazon.lambda.powertools.batch.message;

/**
 * Common base class for all batch message processing message types.
 */
public abstract class BatchProcessorMessage {

    private String eventSourceArn;
    private String eventSource;
    private String awsRegion;

    // Called messageId or eventId depending on source
    private String messageId;

    private String body;

    private String md5OfBody;
}
