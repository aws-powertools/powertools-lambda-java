package software.amazon.lambda.powertools.batch3;


import software.amazon.lambda.powertools.batch3.examples.SqsExampleWithIdempotency;

/**
 * A builder-style interface we can use within an existing Lambda RequestHandler to
 * deal with our batch responses. A second tier of builders is returned per-event-source
 * to bind the appropriate message types and provider source-specific logic and tuneables.
 *
 * @see SqsExampleWithIdempotency
 */
public class BatchMessageHandlerBuilder {

    public SqsBatchMessageHandlerBuilder withSqsBatchHandler() {
        return new SqsBatchMessageHandlerBuilder();
    }

    public DdbMessageHandlerBuilder withDynamoDbBatchHandler() {
        return new DdbMessageHandlerBuilder();
    }

}
