package software.amazon.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.tracing.PowerToolTracing;

public class PowerTracerToolEnabled implements RequestHandler<Object, Object> {

    @Override
    @PowerToolTracing(namespace = "lambdaHandler")
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
