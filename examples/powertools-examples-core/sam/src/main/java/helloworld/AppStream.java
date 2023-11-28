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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class AppStream implements RequestStreamHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final static Logger log = LogManager.getLogger(AppStream.class);

    @Override
    @Logging(logEvent = true)
    @Metrics(namespace = "ServerlessAirline", service = "payment", captureColdStart = true)
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8)))) {

            log.info("Received: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(reader)));

            writer.write("{\"body\": \"" + System.currentTimeMillis() + "\"} ");
        } catch (IOException e) {
            log.error("Something has gone wrong: ", e);
        }
    }
}

