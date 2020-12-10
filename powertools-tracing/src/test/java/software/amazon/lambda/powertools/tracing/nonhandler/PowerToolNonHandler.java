package software.amazon.lambda.powertools.tracing.nonhandler;

import software.amazon.lambda.powertools.tracing.Tracing;

public class PowerToolNonHandler {

    @Tracing
    public void doSomething() {
    }

    @Tracing(segmentName = "custom")
    public void doSomethingCustomName() {
    }
}
