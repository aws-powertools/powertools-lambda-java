package software.amazon.lambda.powertools.cloudformation;

/**
 * Indicates an error attempting to generation serialize a response to be sent to a custom resource.
 */
public class ResponseException extends Exception {

    private static final long serialVersionUID = 20211004;

    protected ResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
