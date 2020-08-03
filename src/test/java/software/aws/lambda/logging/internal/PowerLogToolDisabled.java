package software.aws.lambda.logging.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class PowerLogToolDisabled implements RequestHandler<Object, Object> {

    @Override
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
