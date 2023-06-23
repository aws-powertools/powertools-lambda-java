package software.amazon.lambda.powertools.e2e;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;

public class Function implements RequestHandler<Input, String> {

    private static final Logger LOG = LogManager.getLogger(Function.class);

    @Logging
    public String handleRequest(Input input, Context context) {

        LoggingUtils.appendKeys(input.getKeys());
        LOG.info(input.getMessage());

        return "OK";
    }
}