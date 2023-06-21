package software.amazon.lambda.powertools.batch.message;

/**
 * A message from a kinesis data streams batch.
 *
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/model/Record.html">Java API Documentation</a>
 */
public class KinesisDataStreamsMessage extends BatchProcessorMessage {
    private String eventVersion;
    private String invokeIdentityArn;
    private String kinesisSchemaVersion;
    private String partitionKey;
    private String sequenceNumber;
    private float approximateArrivalTimestamp;
}
