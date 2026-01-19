package software.amazon.lambda.powertools.utilities;

/**
 * Exception thrown when priming operations fail during CRaC checkpoint preparation.
 */
public class PrimingException extends RuntimeException {
    public PrimingException(String message, Exception e) {
        super(message,e);
    }
}