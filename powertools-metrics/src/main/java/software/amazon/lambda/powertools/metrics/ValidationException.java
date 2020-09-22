package software.amazon.lambda.powertools.metrics;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
