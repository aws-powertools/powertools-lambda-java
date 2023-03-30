package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;
import static software.amazon.awscdk.BundlingOutput.NOT_ARCHIVED;

public class PowertoolsExamplesCloudformationCdkStack extends Stack {

    public static final String SAMPLE_BUCKET_NAME = "sample-bucket-name-20230315-abc123";

    public PowertoolsExamplesCloudformationCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public PowertoolsExamplesCloudformationCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        List<String> functionPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "mvn clean install" +
                        "&& mkdir /asset-output/lib" +
                        "&& cp target/powertools-examples-cloudformation-*.jar  /asset-output/lib"
        );
        BundlingOptions bundlingOptions = BundlingOptions.builder()
                .command(functionPackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(NOT_ARCHIVED)
                .build();


        Function helloWorldFunction = new Function(this, "HelloWorldFunction", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../../", AssetOptions.builder().bundling(bundlingOptions)
                        .build()))
                .handler("helloworld.App::handleRequest")
                .memorySize(512)
                .timeout(Duration.seconds(20))
                .environment(Map.ofEntries(entry("JAVA_TOOL_OPTIONS", "-XX:+TieredCompilation -XX:TieredStopAtLevel=1")))
                .build());
        helloWorldFunction.addToRolePolicy(new PolicyStatement(PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(List.of("s3:GetLifecycleConfiguration",
                        "s3:PutLifecycleConfiguration",
                        "s3:CreateBucket",
                        "s3:ListBucket",
                        "s3:DeleteBucket"))
                .resources(List.of("*"))
                .build()));

        CfnParameter bucketName = CfnParameter.Builder.create(this, "BucketNameParam")
                .type("String")
                .defaultValue(SAMPLE_BUCKET_NAME)
                .build();
        CfnParameter retentionDays = CfnParameter.Builder.create(this, "RetentionDaysParam")
                .type("Number")
                .defaultValue(10)
                .build();


        CustomResource.Builder.create(this, "HelloWorldCustomResource")
                .serviceToken(helloWorldFunction.getFunctionArn())
                .properties(Map
                        .of("BucketName", bucketName.getValueAsString(),"RetentionDays", retentionDays.getValueAsNumber()))
                .build();

    }
}
