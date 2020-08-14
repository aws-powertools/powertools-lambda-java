package software.amazon.lambda.handlers;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.lambda.logging.PowerToolsLogging;

public class PowerLogToolEnabledForStream implements RequestStreamHandler {

    @PowerToolsLogging
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {

    }
}
