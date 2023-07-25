package software.amazon.lambda.powertools.e2e;

public class Input {
    private String message;

    public Input(String message) {
        this.message = message;
    }

    public Input() {
        // for deserialization
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
