package cdk;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

/**
 * Defines a stack that consists of a single Java Lambda function and an API Gateway
 */
public class CdkStack extends Stack {
    private static final String SHELL_COMMAND = "/bin/sh";
    private static final String MAVEN_PACKAGE = "mvn package";
    private static final String COPY_OUTPUT = "cp /asset-input/target/helloworld-lambda.jar /asset-output/";

    public CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Function helloWorldFunction = createHelloWorldFunction();
        Function helloWorldStreamFunction = createHelloWorldStreamFunction();
        RestApi restApi = createHelloWorldApi();

        restApi.getRoot().resourceForPath("/hello")
                .addMethod("GET", LambdaIntegration.Builder.create(helloWorldFunction)
                        .build());

        restApi.getRoot().resourceForPath("/hellostream")
                .addMethod("GET", LambdaIntegration.Builder.create(helloWorldStreamFunction)
                        .build());

        outputApiUrl(restApi);
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

    /**
     * Adds URL to the lambda to the outputs
     *
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
                .environment(Map.of("POWERTOOLS_LOG_LEVEL", "INFO",
                        "POWERTOOLS_LOGGER_SAMPLE_RATE", "0.1",
                        "POWERTOOLS_LOGGER_LOG_EVENT", "true",
                        "POWERTOOLS_METRICS_NAMESPACE", "Coreutilities"
                ))
                .build();
    }

    private Function createHelloWorldStreamFunction() {
        List<String> functionPackageInstructions = createFunctionPackageInstructions();

        return Function.Builder.create(this, "HelloWorldStreamFunction")
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
                .handler("helloworld.AppStream")
                .environment(Map.of("POWERTOOLS_LOG_LEVEL", "INFO",
                        "POWERTOOLS_LOGGER_SAMPLE_RATE", "0.7",
                        "POWERTOOLS_LOGGER_LOG_EVENT", "true",
                        "POWERTOOLS_METRICS_NAMESPACE", "Coreutilities",
                        "POWERTOOLS_SERVICE_NAME", "hello"
                ))
                .build();
    }

    // Method to create the REST API
    private RestApi createHelloWorldApi() {
        return RestApi.Builder.create(this, "HelloWorldApi")
                .description("API Gateway endpoint URL for Prod stage for Hello World function")
                .build();
    }
}
