package software.amazon.lambda.powertools.batch;

import software.amazon.lambda.powertools.batch.builder.*;

/**
 * A builder-style interface we can use within an existing Lambda RequestHandler to
 * deal with our batch responses. A second tier of builders is returned per-event-source
 * to bind the appropriate message types and provider source-specific logic and tuneables.
 **/
public class BatchMessageHandlerBuilder {

    /**
     * Build an SQS-batch message handler.
     *
     * @return A fluent builder interface to continue the building
     */
    public SqsBatchMessageHandlerBuilder withSqsBatchHandler() {
        return new SqsBatchMessageHandlerBuilder();
    }

    /**
     * Build a DynamoDB streams batch message handler.
     *
     * @return A fluent builder interface to continue the building
     */
    public DdbBatchMessageHandlerBuilder withDynamoDbBatchHandler() {
        return new DdbBatchMessageHandlerBuilder();
    }

    /**
     * Builds a Kinesis streams batch message handler.
     *
     * @return a fluent builder interface to continue the building
     */
    public KinesisBatchMessageHandlerBuilder withKinesisBatchHandler() {
        return new KinesisBatchMessageHandlerBuilder();
    }
}
