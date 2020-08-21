package software.amazon.lambda.powertools.tracing.handlers;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.lambda.powertools.tracing.PowerToolsTracing;

public class PowerTracerToolEnabledForStream implements RequestStreamHandler {

    @Override
    @PowerToolsTracing(namespace = "streamHandler")
    public void handleRequest(InputStream input, OutputStream output, Context context) {

    }
}
