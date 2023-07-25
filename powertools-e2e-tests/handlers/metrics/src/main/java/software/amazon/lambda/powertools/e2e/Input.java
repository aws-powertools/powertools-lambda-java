package software.amazon.lambda.powertools.e2e;

import java.util.Map;

public class Input {
    private Map<String, Double> metrics;

    private Map<String, String> dimensions;

    public Map<String, Double> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Double> metrics) {
        this.metrics = metrics;
    }

    public Input() {
        // for deserialization
    }


    public Map<String, String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = dimensions;
    }
}
