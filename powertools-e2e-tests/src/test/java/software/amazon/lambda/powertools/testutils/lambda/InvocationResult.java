package software.amazon.lambda.powertools.testutils.lambda;

import java.time.Instant;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.lambda.powertools.testutils.logging.InvocationLogs;

public class InvocationResult {

    private final InvocationLogs logs;
    private final String result;

    private final String requestId;
    private final Instant start;
    private final Instant end;

    public InvocationResult(InvokeResponse response, Instant start, Instant end) {
        requestId = response.responseMetadata().requestId();
        logs = new InvocationLogs(response.logResult(), requestId);
        result = response.payload().asUtf8String();
        this.start = start;
        this.end = end;
    }

    public InvocationLogs getLogs() {
        return logs;
    }

    public String getResult() {
        return result;
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }
}
