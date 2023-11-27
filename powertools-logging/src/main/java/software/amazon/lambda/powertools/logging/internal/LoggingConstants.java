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

public class LoggingConstants {
    public static final String LAMBDA_LOG_LEVEL = System.getenv("AWS_LAMBDA_LOG_LEVEL");

    public static final String LAMBDA_LOG_FORMAT = System.getenv("AWS_LAMBDA_LOG_FORMAT");

    public static final String LOG_DATE_RFC3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private LoggingConstants() {
        // constants
    }
}
