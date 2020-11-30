package software.amazon.lambda.powertools.parameters.exception;

public class ProviderException extends RuntimeException {

    public ProviderException(Exception e) {
        super(e);
    }

    public ProviderException(String message) {
        super(message);
    }
}
