package software.aws.lambda.handlers;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.aws.lambda.tracing.PowerToolTracing;

public class PowerTracerToolEnabledForStreamWithNoMetaData implements RequestStreamHandler {

    @Override
    @PowerToolTracing(captureResponse = false, captureError = false)
    public void handleRequest(InputStream input, OutputStream output, Context context) {

    }
}
