/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package helloworld;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public App() {
        // Flush immediately because no trace ID is set yet
        log.debug("Constructor DEBUG - should not be buffered (no trace ID)");
        log.info("Constructor INFO - should not be buffered (no trace ID)");
    }

    @Logging(logEvent = false)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // Manually set trace ID for testing in SAM local
        System.setProperty("com.amazonaws.xray.traceHeader",
                "Root=1-63441c4a-abcdef012345678912345678;Parent=0123456789abcdef;Sampled=1");

        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        log.debug("DEBUG 1");
        MDC.put("test", "willBeLogged");
        log.debug("DEBUG 2");
        log.info("INFO 1");

        // Manually flush buffer to show buffered debug logs
        PowertoolsLogging.flushBuffer();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", "Test");

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (RuntimeException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

}
