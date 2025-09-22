package software.amazon.lambda.powertools.logging.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.lambda.powertools.logging.Logging;

public class PowertoolsLogErrorNoFlush implements RequestHandler<String, String> {

    @Override
    @Logging(logError = true, flushBufferOnUncaughtError = false)
    public String handleRequest(String input, Context context) {
        throw new RuntimeException("This is an error without buffer flush");
    }
}
