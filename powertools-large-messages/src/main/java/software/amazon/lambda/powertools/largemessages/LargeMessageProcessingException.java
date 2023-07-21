package software.amazon.lambda.powertools.largemessages;

public class LargeMessageProcessingException extends RuntimeException {
    public LargeMessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public LargeMessageProcessingException(String message) {
        super(message);
    }
}
