package software.aws.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class PowerToolDisabled implements RequestHandler<Object, Object> {

    @Override
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
