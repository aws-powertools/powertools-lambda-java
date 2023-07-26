package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

public class Function implements RequestHandler<Input, String> {

    @Tracing
    public String handleRequest(Input input, Context context) {
        try {
            Thread.sleep(100); // simulate stuff
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String message = buildMessage(input.getMessage(), context.getFunctionName());

        TracingUtils.withSubsegment("internal_stuff", subsegment ->
            {
                try {
                    Thread.sleep(100); // simulate stuff
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

        return message;
    }

    @Tracing
    private String buildMessage(String message, String funcName) {
        TracingUtils.putAnnotation("message", message);
        try {
            Thread.sleep(150); // simulate other stuff
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return String.format("%s (%s)", message, funcName);
    }
}