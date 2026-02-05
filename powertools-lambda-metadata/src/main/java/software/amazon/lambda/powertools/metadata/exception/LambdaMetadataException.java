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

package software.amazon.lambda.powertools.metadata.exception;

/**
 * Exception thrown when the Lambda Metadata Endpoint is unavailable or returns an error.
 * <p>
 * This exception may be thrown when:
 * <ul>
 *   <li>The metadata endpoint environment variables are not set</li>
 *   <li>The metadata endpoint returns a non-200 status code</li>
 *   <li>Network errors occur when connecting to the endpoint</li>
 *   <li>The response cannot be parsed</li>
 * </ul>
 * </p>
 */
public class LambdaMetadataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP status code from the metadata endpoint, or -1 if not applicable.
     */
    private final int statusCode;

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the error message
     */
    public LambdaMetadataException(String message) {
        super(message);
        this.statusCode = -1;
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public LambdaMetadataException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    /**
     * Constructs a new exception with the specified message and HTTP status code.
     *
     * @param message    the error message
     * @param statusCode the HTTP status code from the metadata endpoint
     */
    public LambdaMetadataException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code from the metadata endpoint.
     *
     * @return the HTTP status code, or -1 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }
}
