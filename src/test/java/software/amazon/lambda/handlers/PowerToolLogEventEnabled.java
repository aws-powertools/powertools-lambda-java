package software.amazon.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.logging.PowerToolsLogging;

public class PowerToolLogEventEnabled implements RequestHandler<Object, Object> {

    @PowerToolsLogging(logEvent = true)
    @Override
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
