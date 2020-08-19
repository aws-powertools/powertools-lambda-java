package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.lambda.handlers.PowerLogToolEnabled;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LambdaJsonLayoutTest {

    private RequestHandler<Object, Object> handler = new PowerLogToolEnabled();

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IOException {
        initMocks(this);
        setupContext();
        //Make sure file is cleaned up before running full stack logging regression
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldLogInStructuredFormat() throws IOException {
        handler.handleRequest("test", context);

        assertThat(Files.lines(Paths.get("target/logfile.json")))
                .hasSize(1)
                .allSatisfy(line -> assertThat(parseToMap(line))
                        .containsEntry("functionName", "testFunction")
                        .containsEntry("functionVersion", "1")
                        .containsEntry("functionMemorySize", "10")
                        .containsEntry("functionArn", "testArn")
                        .containsKey("timestamp")
                        .containsKey("message")
                        .containsKey("service"));
    }

    private Map<String, Object> parseToMap(String stringAsJson) {
        try {
            return new ObjectMapper().readValue(stringAsJson, Map.class);
        } catch (JsonProcessingException e) {
            fail("Failed parsing logger line " + stringAsJson);
            return emptyMap();
        }
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}