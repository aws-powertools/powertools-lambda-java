package software.amazon.lambda.powertools.e2e;

import java.util.Map;

public class Input {
    private String message;
    private Map<String, String> keys;

    public Input() {
        // for deserialization
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }
}
