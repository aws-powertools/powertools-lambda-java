package software.amazon.lambda.powertools.idempotency.exceptions;

/**
 * Exception that occurs when the Idepmpotency Key is null or could not be found with the provided JMESPath
 */
public class NullIdempotencyKeyException extends Exception {
    private static final long serialVersionUID = 5115004524004542891L;

    public NullIdempotencyKeyException(String message) {
        super(message);
    }
}
