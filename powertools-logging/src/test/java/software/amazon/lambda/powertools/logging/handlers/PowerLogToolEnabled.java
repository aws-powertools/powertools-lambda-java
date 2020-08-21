package software.amazon.lambda.powertools.logging.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.PowerToolsLogging;

public class PowerLogToolEnabled implements RequestHandler<Object, Object> {
    private final Logger LOG = LogManager.getLogger(PowerToolLogEventEnabled.class);

    @Override
    @PowerToolsLogging
    public Object handleRequest(Object input, Context context) {
        LOG.info("Test event");
        return null;
    }

    @PowerToolsLogging
    public void anotherMethod() {
        System.out.println("test");
    }
}
