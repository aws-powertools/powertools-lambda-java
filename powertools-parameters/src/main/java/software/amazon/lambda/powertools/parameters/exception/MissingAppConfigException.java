package software.amazon.lambda.powertools.parameters.exception;

public class MissingAppConfigException extends RuntimeException {
    public MissingAppConfigException(String msg) {
        super(msg);
    }

}
