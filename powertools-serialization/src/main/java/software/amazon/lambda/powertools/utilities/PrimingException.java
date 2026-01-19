package software.amazon.lambda.powertools.utilities;

/**
 * Exception thrown when priming operations fail during CRaC checkpoint preparation.
 */
public class PrimingException extends Exception {

    public PrimingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrimingException(String message) {
        super(message);
    }
}