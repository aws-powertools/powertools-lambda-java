package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import software.amazon.lambda.powertools.common.internal.SystemWrapper;

public final class LambdaResource {

    private LambdaResource() {
    }

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