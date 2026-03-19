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

package software.amazon.lambda.powertools.metadata.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.lambda.powertools.metadata.LambdaMetadata;
import software.amazon.lambda.powertools.metadata.exception.LambdaMetadataException;

/**
 * Internal HTTP client for fetching metadata from the Lambda Metadata Endpoint.
 * <p>
 * Uses {@link HttpURLConnection} to avoid additional dependencies - it's part of the JDK
 * and works out of the box with GraalVM native image.
 * </p>
 */
public class LambdaMetadataHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaMetadataHttpClient.class);

    static final String ENV_METADATA_API = "AWS_LAMBDA_METADATA_API";
    static final String ENV_METADATA_TOKEN = "AWS_LAMBDA_METADATA_TOKEN";
    private static final String API_VERSION = "2026-01-15";
    private static final String METADATA_PATH = "/metadata/execution-environment";
    private static final int CONNECT_TIMEOUT_MS = 1000;
    private static final int READ_TIMEOUT_MS = 1000;

    private final ObjectMapper objectMapper;

    public LambdaMetadataHttpClient() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches metadata from the Lambda Metadata Endpoint.
     *
     * @return the Lambda metadata
     * @throws LambdaMetadataException if the fetch fails
     */
    public LambdaMetadata fetchMetadata() {
        String token = getRequiredEnvironmentVariable(ENV_METADATA_TOKEN);
        String api = getRequiredEnvironmentVariable(ENV_METADATA_API);

        String urlString = "http://" + api + "/" + API_VERSION + METADATA_PATH;
        LOG.debug("Fetching Lambda metadata from: {}", urlString);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoInput(true);

            int statusCode = conn.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                String errorMessage = readErrorStream(conn);
                throw new LambdaMetadataException(
                        "Metadata request failed with status " + statusCode + ": " + errorMessage,
                        statusCode);
            }

            String responseBody = readInputStream(conn);
            LOG.debug("Lambda metadata response: {}", responseBody);

            return objectMapper.readValue(responseBody, LambdaMetadata.class);
        } catch (LambdaMetadataException e) {
            throw e;
        } catch (IOException e) {
            throw new LambdaMetadataException("Failed to fetch Lambda metadata: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Gets a required environment variable value.
     * Throws {@link LambdaMetadataException} if the value is null or empty.
     * This method is package-private to allow overriding in tests.
     *
     * @param name the environment variable name
     * @return the value, never null or empty
     * @throws LambdaMetadataException if the environment variable is not set or empty
     */
    String getRequiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new LambdaMetadataException(
                    "Environment variable " + name + " is not set. Ensure " + name + " is set.");
        }
        return value;
    }

    /**
     * Reads the input stream from a connection.
     *
     * @param conn the HTTP connection
     * @return the response body as a string
     * @throws IOException if reading fails
     */
    private String readInputStream(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Reads the error stream from a connection.
     *
     * @param conn the HTTP connection
     * @return the error message, or empty string if not available
     */
    private String readErrorStream(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to read error stream", e);
        }
        return "";
    }
}
