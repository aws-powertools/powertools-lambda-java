package software.amazon.lambda.powertools.testsuite.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

public class TracingLoggingStreamMessageHandler implements RequestStreamHandler {

    @Logging(logEvent = true)
    @Tracing
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(output, mapper.readValue(input, Map.class));
    }
}
