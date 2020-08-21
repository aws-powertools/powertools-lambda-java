package software.amazon.lambda.powertools.tracing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.powertools.tracing.PowerToolsTracing;

public class PowerTracerToolEnabledWithException implements RequestHandler<Object, Object> {

    @Override
    @PowerToolsTracing(namespace = "lambdaHandler")
    public Object handleRequest(Object input, Context context) {
        throw new RuntimeException("I am failing!");
    }
}
