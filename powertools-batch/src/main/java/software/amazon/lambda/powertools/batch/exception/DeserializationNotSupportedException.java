package software.amazon.lambda.powertools.batch.exception;

public class DeserializationNotSupportedException extends RuntimeException {

    public DeserializationNotSupportedException() {
        super("This BatchMessageHandler has a fixed schema and does not support user-defined types");
    }

}
