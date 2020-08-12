package software.aws.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.aws.lambda.logging.PowerToolsLogging;

public class PowerLogToolEnabled implements RequestHandler<Object, Object> {

    @Override
    @PowerToolsLogging
    public Object handleRequest(Object input, Context context) {
        return null;
    }

    @PowerToolsLogging
    public void anotherMethod() {

    }
}
