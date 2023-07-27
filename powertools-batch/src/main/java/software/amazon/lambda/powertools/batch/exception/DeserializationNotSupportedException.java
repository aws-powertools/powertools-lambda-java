package software.amazon.lambda.powertools.batch.exception;

/**
 * Thrown by message handlers that do not support deserializing arbitrary payload
 * contents. This is the case for instance with DynamoDB Streams, which stream
 * changesets about user-defined data, but not the user-defined data models themselves.
 */
public class DeserializationNotSupportedException extends RuntimeException {

    public DeserializationNotSupportedException() {
        super("This BatchMessageHandler has a fixed schema and does not support user-defined types");
    }

}
