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

package software.amazon.lambda.powertools.logging.internal;

public final class LoggingConstants {
    @SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
    public static String LAMBDA_LOG_LEVEL = System.getenv("AWS_LAMBDA_LOG_LEVEL"); /* not final for test purpose */

    @SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
    public static String POWERTOOLS_LOG_LEVEL = System.getenv("POWERTOOLS_LOG_LEVEL"); /* not final for test purpose */

    @SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
    public static String POWERTOOLS_SAMPLING_RATE = System.getenv("POWERTOOLS_LOGGER_SAMPLE_RATE"); /* not final for test purpose */

    static boolean POWERTOOLS_LOG_EVENT = "true".equals(System.getenv("POWERTOOLS_LOGGER_LOG_EVENT")); /* not final for test purpose */

    static boolean POWERTOOLS_LOG_RESPONSE = "true".equals(System.getenv("POWERTOOLS_LOGGER_LOG_RESPONSE")); /* not final for test purpose */

    static boolean POWERTOOLS_LOG_ERROR = "true".equals(System.getenv("POWERTOOLS_LOGGER_LOG_ERROR")); /* not final for test purpose */

    private LoggingConstants() {
        // constants
    }
}
