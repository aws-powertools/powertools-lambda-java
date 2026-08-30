/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

/**
 * A utility class that holds constant values for various attribute names and configurations
 * used in AWS Lambda and Powertools for AWS Lambda. These constants are mainly used for
 * telemetry, tracing, and environment variable configuration within the application.
 * <p>
 * This class is designed as a final class with a private constructor to prevent instantiation
 * and ensure it acts solely as a container for constants.
 */
public final class AttributesConstants {

    private AttributesConstants() {
        // Constant holder class
    }

    public static final String AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME";

    public static final String AWS_LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION";

    public static final String AWS_LAMBDA_FUNCTION_MEMORY_SIZE = "AWS_LAMBDA_FUNCTION_MEMORY_SIZE";

    public static final String AWS_LAMBDA_LOG_STREAM_NAME = "AWS_LAMBDA_LOG_STREAM_NAME";

    public static final String AWS_REGION = "AWS_REGION";

    public static final String AWS_LAMBDA_FUNCTION_ARN = "AWS_LAMBDA_FUNCTION_ARN";

    public static final String TELEMETRY_DISTRO_NAME = "powertools-for-aws-lambda";

    public static final String FAAS_COLDSTART = "faas.coldstart";

    public static final String FAAS_INVOCATION_ID = "faas.invocation_id";

    public static final String RESPONSE_ATTRIBUTE = "aws.lambda.powertools.response";

    public static final String CAPTURE_RESPONSE_ENV = "POWERTOOLS_TRACER_CAPTURE_RESPONSE";

    public static final String CAPTURE_ERROR_ENV = "POWERTOOLS_TRACER_CAPTURE_ERROR";

    public static final String TRACEPARENT = "traceparent";

    public static final String TRACESTATE = "tracestate";
}
