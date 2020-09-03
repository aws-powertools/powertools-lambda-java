package software.amazon.lambda.powertools.parameters.exception;

public class TransformationException extends RuntimeException {

    public TransformationException(Exception e) {
        super(e);
    }

    public TransformationException(String message) { super(message);
    }
}
