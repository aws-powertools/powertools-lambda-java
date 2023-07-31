package cdk;


import software.amazon.awscdk.BundlingOptions;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CdkStack extends Stack {
    public CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // CDK will use this command to package your Java Lambda
        List<String> functionPackageInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "mvn package " +
                        "&& cp /asset-input/target/powertools-examples-core-cdk-1.16.1.jar /asset-output/"
        );

        final Function helloWorldFunction = Function.Builder.create(this, "HelloWorldFunction")
                .runtime(Runtime.JAVA_11)    // execution environment
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
                .environment(new HashMap<String, String>() {{
                    put("POWERTOOLS_LOG_LEVEL", "0.1");
                    put("POWERTOOLS_LOGGER_SAMPLE_RATE", "INFO");
                    put("POWERTOOLS_LOGGER_LOG_EVENT", "true");
                    put("POWERTOOLS_METRICS_NAMESPACE", "Coreutilities");
                }})
                .build();

        RestApi reastApi = RestApi.Builder.create(this, "HelloWorldApi")
                .description("API Gateway endpoint URL for Prod stage for Hello World function")
                .build();

        reastApi.getRoot().resourceForPath("/hello")
                .addMethod("GET", LambdaIntegration.Builder.create(helloWorldFunction)
                        .build());

    }
}
