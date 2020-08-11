package software.aws.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.aws.lambda.tracing.PowerToolTracing;

public class PowerTracerToolEnabledWithNoMetaData implements RequestHandler<Object, Object> {

    @Override
    @PowerToolTracing(captureResponse = false, captureError = false)
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
