package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class AppStream implements RequestStreamHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    @PowertoolsLogging(logEvent = true)
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        Map map = mapper.readValue(input, Map.class);

        System.out.println(map.size());
    }
}
