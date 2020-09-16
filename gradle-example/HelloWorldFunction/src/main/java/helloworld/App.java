package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.PowertoolsLogger;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.tracing.PowerTracer;
import software.amazon.lambda.powertools.tracing.PowertoolsTracing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static software.amazon.lambda.powertools.tracing.PowerTracer.putMetadata;
import static software.amazon.lambda.powertools.tracing.PowerTracer.withEntitySubsegment;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    Logger log = LogManager.getLogger();

    @PowertoolsLogging(logEvent = true, samplingRate = 0.7)
    @PowertoolsTracing(captureError = false, captureResponse = false)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        PowertoolsLogger.appendKey("test", "willBeLogged");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
            log.info(pageContents);
            PowerTracer.putAnnotation("Test", "New");
            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);

            PowerTracer.withSubsegment("loggingResponse", subsegment -> {
                String sampled = "log something out";
                log.info(sampled);
                log.info(output);
            });

            threadOption1();

            threadOption2();

            log.info("After output");
            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (IOException | InterruptedException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private void threadOption1() throws InterruptedException {
        Entity traceEntity = AWSXRay.getTraceEntity();
        Thread thread = new Thread(() -> {
            AWSXRay.setTraceEntity(traceEntity);
            log();
        });
        thread.start();
        thread.join();
    }

    private void threadOption2() throws InterruptedException {
        Entity traceEntity = AWSXRay.getTraceEntity();
        Thread anotherThread = new Thread(() -> withEntitySubsegment("inlineLog", traceEntity, subsegment -> {
            String var = "somethingToProcess";
            log.info("inside threaded logging inline {}", var);
        }));
        anotherThread.start();
        anotherThread.join();
    }

    @PowertoolsTracing
    private void log() {
        log.info("inside threaded logging for function");
    }


    @PowertoolsTracing(namespace = "getPageContents", captureResponse = false, captureError = false)
    private String getPageContents(String address) throws IOException {
        URL url = new URL(address);
        putMetadata("getPageContents", address);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
