package software.amazon.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.tracing.PowerToolsTracing;

public class PowerTracerToolEnabledWithNoMetaData implements RequestHandler<Object, Object> {

    @Override
    @PowerToolsTracing(captureResponse = false, captureError = false)
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
