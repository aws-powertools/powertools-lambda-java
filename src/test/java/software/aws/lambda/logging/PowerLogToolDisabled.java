package software.aws.lambda.logging;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.junit.jupiter.api.Test;

public class PowerLogToolDisabled implements RequestHandler<Object, Object> {

    @Override
    @PowerToolsLogging(injectContextInfo = false)
    public Object handleRequest(Object input, Context context) {
        return null;
    }
}
