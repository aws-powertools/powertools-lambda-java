package cdk;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * Defines a stack that consists of a single Java Lambda function and an API Gateway
 */
public class CdkStack extends Stack {
    private static final String SHELL_COMMAND = "/bin/sh";
    private static final String MAVEN_PACKAGE = "mvn package";
    private static final String COPY_OUTPUT = "cp /asset-input/target/powertools-examples-core-cdk-1.16.1.jar /asset-output/";

    public CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Function helloWorldFunction = createHelloWorldFunction();
        RestApi restApi = createHelloWorldApi();

        restApi.getRoot().resourceForPath("/hello")
                .addMethod("GET", LambdaIntegration.Builder.create(helloWorldFunction)
                        .build());

        outputApiUrl(restApi);
    }

    /**
     * Adds URL to the lambda to the outputs
     * @param restApi
     */
    private void outputApiUrl(RestApi restApi) {
        CfnOutput.Builder.create(this, "HelloWorldApiUrl")
                .description("API Gateway endpoint URL for Prod stage for Hello World function")
                .value(restApi.getUrl() + "hello").build();
    }

    // Method to create the Lambda function
    private Function createHelloWorldFunction() {
        List<String> functionPackageInstructions = createFunctionPackageInstructions();

        return Function.Builder.create(this, "HelloWorldFunction")
                .runtime(Runtime.JAVA_11)
                .memorySize(512)
                .timeout(Duration.seconds(20))
                .tracing(Tracing.ACTIVE)
                .code(Code.fromAsset("../app/", AssetOptions.builder()
                        .bundling(BundlingOptions.builder()
                                .image(Runtime.JAVA_11.getBundlingImage())
                                .command(functionPackageInstructions)
                                .build())
                        .build()))
                .handler("helloworld.App")
                .environment(Map.of("POWERTOOLS_LOG_LEVEL", "0.1",
                        "POWERTOOLS_LOGGER_SAMPLE_RATE", "INFO",
                        "POWERTOOLS_LOGGER_LOG_EVENT", "true",
                        "POWERTOOLS_METRICS_NAMESPACE", "Coreutilities"
                ))
                .build();
    }

    // Method to create the REST API
    private RestApi createHelloWorldApi() {
        return RestApi.Builder.create(this, "HelloWorldApi")
                .description("API Gateway endpoint URL for Prod stage for Hello World function")
                .build();
    }

    private static List<String> createFunctionPackageInstructions() {
        // CDK will use this command to package your Java Lambda
        return List.of(
                SHELL_COMMAND,
                "-c",
                MAVEN_PACKAGE + " && " +
                        COPY_OUTPUT
        );
    }
}
