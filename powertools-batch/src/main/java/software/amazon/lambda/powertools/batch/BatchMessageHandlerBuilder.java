package software.amazon.lambda.powertools.batch;

import software.amazon.lambda.powertools.batch.builder.*;

/**
 * A builder-style interface we can use to build batch processing handlers for SQS, Kinesis Streams,
 * and DynamoDB Streams batches. The batch processing handlers that are returned allow
 * the user to easily process batches of messages, one-by-one, while offloading
 * the common issues - failure handling, partial responses, deserialization -
 * to the library.
 *
 * @see <a href="https://docs.powertools.aws.dev/lambda/java/utilities/batch/">Powertools for AWS Lambda (Java) Batch Documentation</a>
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
    public DynamoDbBatchMessageHandlerBuilder withDynamoDbBatchHandler() {
        return new DynamoDbBatchMessageHandlerBuilder();
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
