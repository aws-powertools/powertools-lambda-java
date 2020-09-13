package helloworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.metrics.PowertoolsMetrics;

public class AppStream implements RequestStreamHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    @PowertoolsLogging(logEvent = true)
    @PowertoolsMetrics(namespace = "ServerlessAirline", service = "payment", captureColdStart = true)
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        Map map = mapper.readValue(input, Map.class);

        System.out.println(map.size());
    }
}
