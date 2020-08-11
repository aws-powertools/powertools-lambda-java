package software.aws.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.aws.lambda.tracing.PowerToolTracing;

public class PowerTracerToolEnabledWithException implements RequestHandler<Object, Object> {

    @Override
    @PowerToolTracing(namespace = "lambdaHandler")
    public Object handleRequest(Object input, Context context) {
        throw new RuntimeException("I am failing!");
    }
}
