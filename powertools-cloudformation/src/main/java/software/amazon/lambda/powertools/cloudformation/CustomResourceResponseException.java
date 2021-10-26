package software.amazon.lambda.powertools.cloudformation;

/**
 * Indicates an error while generating or serializing a response to be sent to a custom resource.
 */
public class CustomResourceResponseException extends Exception {

    private static final long serialVersionUID = 20211004;

    protected CustomResourceResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
