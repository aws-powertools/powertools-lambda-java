package software.amazon.lambda.powertools.batch.message;

public enum BatchProcessorMessageType {
    Sqs,
    DynamoDbStreams,
    KinesisDataStreams
}
