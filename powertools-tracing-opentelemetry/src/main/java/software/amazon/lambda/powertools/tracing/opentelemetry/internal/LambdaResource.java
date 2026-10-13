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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;

/**
 * The {@code LambdaResource} class is a utility for creating a representation of
 * an AWS Lambda execution environment in the form of a {@code Resource} object.
 * It extracts and structures metadata about the Lambda function's runtime environment,
 * which is useful for telemetry and observability purposes.
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 * - Populates resource attributes based on AWS Lambda-specific environment variables.
 * - Includes attributes related to the cloud provider, service, function details, and
 *   OpenTelemetry metadata.
 * - Processes function execution context such as memory size and account ID from the
 *   AWS Lambda environment.
 * - Ensures only relevant and non-empty values are added as attributes.
 * <p>
 * This class is designed to be final and non-instantiable, serving purely as a
 * container for a static method.
 */
public final class LambdaResource {

    private LambdaResource() {
    }

    /**
     * Creates a Resource instance populated with attributes derived from the
     * AWS Lambda environment. The attributes include cloud provider information,
     * service details, function memory size, account ID, and OpenTelemetry metadata.
     * <p>
     * It retrieves environment variables specific to AWS Lambda and processes
     * them to build a comprehensive resource description.
     *
     * @return a Resource object containing attributes about the AWS Lambda environment
     */
    public static Resource create() {
        AttributesBuilder attributes = Attributes.builder();

        putIfPresent(
                attributes,
                "cloud.provider",
                "aws"
        );

        putIfPresent(
                attributes,
                "cloud.region",
                SystemWrapper.getenv(AttributesConstants.AWS_REGION)
        );

        putIfPresent(
                attributes,
                "service.name",
                SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_FUNCTION_NAME)
        );

        putIfPresent(
                attributes,
                "service.version",
                SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_FUNCTION_VERSION)
        );

        putIfPresent(
                attributes,
                "faas.name",
                SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_FUNCTION_NAME)
        );

        putIfPresent(
                attributes,
                "faas.version",
                SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_FUNCTION_VERSION)
        );

        putIfPresent(
                attributes,
                "faas.instance",
                SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_LOG_STREAM_NAME)
        );

        String memory = SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_FUNCTION_MEMORY_SIZE);

        if (memory != null) {
            attributes.put(
                    "faas.max_memory",
                    Long.parseLong(memory)
            );
        }

        String functionArn = SystemWrapper.getenv(AttributesConstants.AWS_LAMBDA_FUNCTION_ARN);

        if (functionArn != null) {
            String accountId = extractAccountId(functionArn);

            if (accountId != null) {
                attributes.put(
                        "cloud.account.id",
                        accountId
                );
            }
        }

        attributes.put(
                "telemetry.sdk.name",
                "opentelemetry"
        );

        attributes.put(
                "telemetry.distro.name",
                AttributesConstants.TELEMETRY_DISTRO_NAME
        );

        attributes.put(
                "telemetry.sdk.language",
                "java"
        );

        return Resource.create(attributes.build());
    }

    private static void putIfPresent(
            AttributesBuilder attributes,
            String key,
            String value) {

        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    private static String extractAccountId(String arn) {
        String[] parts = arn.split(":");

        return parts.length > 4
                ? parts[4]
                : null;
    }
}