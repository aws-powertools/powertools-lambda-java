package software.amazon.lambda.powertools.batch;

public class TooManyMessageHandlersException extends RuntimeException {

    public TooManyMessageHandlersException() {
        super("You must configure either a rawMessageHandler or a messageHandler - not both");
    }

}
