package software.amazon.lambda.powertools.tracing.opentelemetry.internal;

public final class AttributesConstants {

    private AttributesConstants() {
        // Constant holder class
    }

    public static final String AWS_LAMBDA_FUNCTION_NAME =
            "AWS_LAMBDA_FUNCTION_NAME";

    public static final String AWS_LAMBDA_FUNCTION_VERSION =
            "AWS_LAMBDA_FUNCTION_VERSION";

    public static final String AWS_LAMBDA_FUNCTION_MEMORY_SIZE =
            "AWS_LAMBDA_FUNCTION_MEMORY_SIZE";

    public static final String AWS_LAMBDA_LOG_STREAM_NAME =
            "AWS_LAMBDA_LOG_STREAM_NAME";

    public static final String AWS_REGION =
            "AWS_REGION";

    public static final String AWS_LAMBDA_FUNCTION_ARN =
            "AWS_LAMBDA_FUNCTION_ARN";

    public static final String TELEMETRY_DISTRO_NAME =
            "powertools-for-aws-lambda";

    public static final String FAAS_COLDSTART = "faas.coldstart";
    
    public static final String FAAS_INVOCATION_ID = "faas.invocation_id";

    public static final String RESPONSE_ATTRIBUTE =
            "aws.lambda.powertools.response";

    public static final String CAPTURE_RESPONSE_ENV =
            "POWERTOOLS_TRACER_CAPTURE_RESPONSE";

    public static final String CAPTURE_ERROR_ENV =
            "POWERTOOLS_TRACER_CAPTURE_ERROR";
}
