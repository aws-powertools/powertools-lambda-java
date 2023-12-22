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

package software.amazon.lambda.powertools.testutils;

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import software.amazon.awscdk.App;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.BundlingOutput;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.cxapi.CloudAssembly;
import software.amazon.awscdk.services.appconfig.CfnApplication;
import software.amazon.awscdk.services.appconfig.CfnConfigurationProfile;
import software.amazon.awscdk.services.appconfig.CfnDeployment;
import software.amazon.awscdk.services.appconfig.CfnDeploymentStrategy;
import software.amazon.awscdk.services.appconfig.CfnEnvironment;
import software.amazon.awscdk.services.appconfig.CfnHostedConfigurationVersion;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.kinesis.StreamMode;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.KinesisEventSource;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * This class is in charge of bootstrapping the infrastructure for the tests.
 * <br/>
 * Tests are actually run on AWS, so we need to provision Lambda functions, DynamoDB table (for Idempotency),
 * CloudWatch log groups, ...
 * <br/>
 * It uses the Cloud Development Kit (CDK) to define required resources. The CDK stack is then synthesized to retrieve
 * the CloudFormation templates and the assets (function jars). Assets are uploaded to S3 (with the SDK `PutObjectRequest`)
 * and the CloudFormation stack is created (with the SDK `createStack`)
 */
public class Infrastructure {
    public static final String FUNCTION_NAME_OUTPUT = "functionName";
    private static final Logger LOG = LoggerFactory.getLogger(Infrastructure.class);

    private final String stackName;
    private final boolean tracing;
    private final Map<String, String> envVar;
    private final JavaRuntime runtime;
    private final App app;
    private final Stack stack;
    private final long timeout;
    private final String pathToFunction;
    private final S3Client s3;
    private final CloudFormationClient cfn;
    private final Region region;
    private final String account;
    private final String idempotencyTable;
    private final AppConfig appConfig;
    private final SdkHttpClient httpClient;
    private final String queue;
    private final String kinesisStream;
    private final String largeMessagesBucket;
    private String ddbStreamsTableName;
    private String functionName;
    private Object cfnTemplate;
    private String cfnAssetDirectory;

    private Infrastructure(Builder builder) {
        this.stackName = builder.stackName;
        this.tracing = builder.tracing;
        this.envVar = builder.environmentVariables;
        this.runtime = builder.runtime;
        this.timeout = builder.timeoutInSeconds;
        this.pathToFunction = builder.pathToFunction;
        this.idempotencyTable = builder.idemPotencyTable;
        this.appConfig = builder.appConfig;
        this.queue = builder.queue;
        this.kinesisStream = builder.kinesisStream;
        this.largeMessagesBucket = builder.largeMessagesBucket;
        this.ddbStreamsTableName = builder.ddbStreamsTableName;

        this.app = new App();
        this.stack = createStackWithLambda();

        this.synthesize();

        this.httpClient = AwsCrtHttpClient.builder().build();
        this.region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
        this.account = StsClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build().getCallerIdentity().account();

        s3 = S3Client.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
        cfn = CloudFormationClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Use the CloudFormation SDK to create the stack
     *
     * @return the name of the function deployed part of the stack
     */
    public Map<String, String> deploy() {
        uploadAssets();
        LOG.info("Deploying '" + stackName + "' on account " + account);
        cfn.createStack(CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(new Yaml().dump(cfnTemplate))
                .timeoutInMinutes(10)
                .onFailure(OnFailure.ROLLBACK)
                .capabilities(Capability.CAPABILITY_IAM)
                .build());
        WaiterResponse<DescribeStacksResponse> waiterResponse =
                cfn.waiter().waitUntilStackCreateComplete(DescribeStacksRequest.builder().stackName(stackName).build());
        if (waiterResponse.matched().response().isPresent()) {
            software.amazon.awssdk.services.cloudformation.model.Stack deployedStack =
                    waiterResponse.matched().response().get().stacks().get(0);
            LOG.info("Stack " + deployedStack.stackName() + " successfully deployed");
            Map<String, String> outputs = new HashMap<>();
            deployedStack.outputs().forEach(output -> outputs.put(output.outputKey(), output.outputValue()));
            return outputs;
        } else {
            throw new RuntimeException("Failed to create stack");
        }
    }

    /**
     * Destroy the CloudFormation stack
     */
    public void destroy() {
        LOG.info("Deleting '" + stackName + "' on account " + account);
        cfn.deleteStack(DeleteStackRequest.builder().stackName(stackName).build());
    }

    /**
     * Build the CDK Stack containing the required resources (Lambda function, LogGroup, DDB Table)
     *
     * @return the CDK stack
     */
    private Stack createStackWithLambda() {
        boolean createTableForAsyncTests = false;
        Stack stack = new Stack(app, stackName);

        List<String> packagingInstruction = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd " + pathToFunction +
                        " && timeout -s SIGKILL 5m mvn clean install -ff " +
                        " -Dmaven.test.skip=true " +
                        " -Dmaven.compiler.source=" + runtime.getMvnProperty() +
                        " -Dmaven.compiler.target=" + runtime.getMvnProperty() +
                        " && cp /asset-input/" + pathToFunction + "/target/function.jar /asset-output/"
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(packagingInstruction)
                .image(runtime.getCdkRuntime().getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(BundlingOutput.ARCHIVED);

        functionName = stackName + "-function";
        CfnOutput.Builder.create(stack, FUNCTION_NAME_OUTPUT)
                .value(functionName)
                .build();

        LOG.debug("Building Lambda function with command " +
                packagingInstruction.stream().collect(Collectors.joining(" ", "[", "]")));
        Function function = Function.Builder
                .create(stack, functionName)
                .code(Code.fromAsset("handlers/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(packagingInstruction)
                                .build())
                        .build()))
                .functionName(functionName)
                .handler("software.amazon.lambda.powertools.e2e.Function::handleRequest")
                .memorySize(1024)
                .timeout(Duration.seconds(timeout))
                .runtime(runtime.getCdkRuntime())
                .environment(envVar)
                .tracing(tracing ? Tracing.ACTIVE : Tracing.DISABLED)
                .build();

        LogGroup.Builder
                .create(stack, functionName + "-logs")
                .logGroupName("/aws/lambda/" + functionName)
                .retention(RetentionDays.ONE_DAY)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        if (!StringUtils.isEmpty(idempotencyTable)) {
            Table table = Table.Builder
                    .create(stack, "IdempotencyTable")
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
                    .tableName(idempotencyTable)
                    .timeToLiveAttribute("expiration")
                    .build();
            function.addEnvironment("IDEMPOTENCY_TABLE", idempotencyTable);

            table.grantReadWriteData(function);
        }

        if (!StringUtils.isEmpty(queue)) {
            Queue sqsQueue = Queue.Builder
                    .create(stack, "SQSQueue")
                    .queueName(queue)
                    .visibilityTimeout(Duration.seconds(timeout * 6))
                    .retentionPeriod(Duration.seconds(timeout * 6))
                    .build();
            DeadLetterQueue.builder()
                    .queue(sqsQueue)
                    .maxReceiveCount(1) // do not retry in case of error
                    .build();
            sqsQueue.grantConsumeMessages(function);
            SqsEventSource sqsEventSource = SqsEventSource.Builder
                    .create(sqsQueue)
                    .enabled(true)
                    .reportBatchItemFailures(true)
                    .batchSize(1)
                    .build();
            function.addEventSource(sqsEventSource);
            CfnOutput.Builder
                    .create(stack, "QueueURL")
                    .value(sqsQueue.getQueueUrl())
                    .build();
            createTableForAsyncTests = true;
        }
        if (!StringUtils.isEmpty(kinesisStream)) {
            Stream stream = Stream.Builder
                    .create(stack, "KinesisStream")
                    .streamMode(StreamMode.ON_DEMAND)
                    .streamName(kinesisStream)
                    .build();

            stream.grantRead(function);
            KinesisEventSource kinesisEventSource = KinesisEventSource.Builder
                    .create(stream)
                    .enabled(true)
                    .batchSize(3)
                    .reportBatchItemFailures(true)
                    .startingPosition(StartingPosition.TRIM_HORIZON)
                    .maxBatchingWindow(Duration.seconds(1))
                    .build();
            function.addEventSource(kinesisEventSource);
            CfnOutput.Builder
                    .create(stack, "KinesisStreamName")
                    .value(stream.getStreamName())
                    .build();
        }

        if (!StringUtils.isEmpty(ddbStreamsTableName)) {
            Table ddbStreamsTable = Table.Builder.create(stack, "DDBStreamsTable")
                    .tableName(ddbStreamsTableName)
                    .stream(StreamViewType.KEYS_ONLY)
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
                    .build();

            DynamoEventSource ddbEventSource = DynamoEventSource.Builder.create(ddbStreamsTable)
                    .batchSize(1)
                    .startingPosition(StartingPosition.TRIM_HORIZON)
                    .maxBatchingWindow(Duration.seconds(1))
                    .reportBatchItemFailures(true)
                    .build();
            function.addEventSource(ddbEventSource);
            CfnOutput.Builder.create(stack, "DdbStreamsTestTable").value(ddbStreamsTable.getTableName()).build();
        }

        if (!StringUtils.isEmpty(largeMessagesBucket)) {
            Bucket offloadBucket = Bucket.Builder
                    .create(stack, "LargeMessagesOffloadBucket")
                    .removalPolicy(RemovalPolicy.RETAIN) // autodelete does not work without cdk deploy
                    .bucketName(largeMessagesBucket)
                    .build();
            // instead of autodelete, have a lifecycle rule to delete files after a day
            LifecycleRule.builder().expiration(Duration.days(1)).enabled(true).build();
            offloadBucket.grantReadWrite(function);
        }

        if (appConfig != null) {
            CfnApplication app = CfnApplication.Builder
                    .create(stack, "AppConfigApp")
                    .name(appConfig.getApplication())
                    .build();

            CfnEnvironment environment = CfnEnvironment.Builder
                    .create(stack, "AppConfigEnvironment")
                    .applicationId(app.getRef())
                    .name(appConfig.getEnvironment())
                    .build();

            // Create a fast deployment strategy, so we don't have to wait ages
            CfnDeploymentStrategy fastDeployment = CfnDeploymentStrategy.Builder
                    .create(stack, "AppConfigDeployment")
                    .name("FastDeploymentStrategy")
                    .deploymentDurationInMinutes(0)
                    .finalBakeTimeInMinutes(0)
                    .growthFactor(100)
                    .replicateTo("NONE")
                    .build();

            // Get the lambda permission to use AppConfig
            function.addToRolePolicy(PolicyStatement.Builder
                    .create()
                    .actions(singletonList("appconfig:*"))
                    .resources(singletonList("*"))
                    .build()
            );

            CfnDeployment previousDeployment = null;
            for (Map.Entry<String, String> entry : appConfig.getConfigurationValues().entrySet()) {
                CfnConfigurationProfile configProfile = CfnConfigurationProfile.Builder
                        .create(stack, "AppConfigProfileFor" + entry.getKey())
                        .applicationId(app.getRef())
                        .locationUri("hosted")
                        .name(entry.getKey())
                        .build();

                CfnHostedConfigurationVersion configVersion = CfnHostedConfigurationVersion.Builder
                        .create(stack, "AppConfigHostedVersionFor" + entry.getKey())
                        .applicationId(app.getRef())
                        .contentType("text/plain")
                        .configurationProfileId(configProfile.getRef())
                        .content(entry.getValue())
                        .build();

                CfnDeployment deployment = CfnDeployment.Builder
                        .create(stack, "AppConfigDepoymentFor" + entry.getKey())
                        .applicationId(app.getRef())
                        .environmentId(environment.getRef())
                        .deploymentStrategyId(fastDeployment.getRef())
                        .configurationProfileId(configProfile.getRef())
                        .configurationVersion(configVersion.getRef())
                        .build();

                // We need to chain the deployments to keep CFN happy
                if (previousDeployment != null) {
                    deployment.addDependency(previousDeployment);
                }
                previousDeployment = deployment;
            }
        }
        if (createTableForAsyncTests) {
            Table table = Table.Builder
                    .create(stack, "TableForAsyncTests")
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .removalPolicy(RemovalPolicy.DESTROY)
                    .partitionKey(Attribute.builder().name("functionName").type(AttributeType.STRING).build())
                    .sortKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
                    .build();

            table.grantReadWriteData(function);
            function.addEnvironment("TABLE_FOR_ASYNC_TESTS", table.getTableName());
            CfnOutput.Builder.create(stack, "TableNameForAsyncTests").value(table.getTableName()).build();
        }

        return stack;
    }

    /**
     * cdk synth to retrieve the CloudFormation template and assets directory
     */
    private void synthesize() {
        CloudAssembly synth = app.synth();
        cfnTemplate = synth.getStackByName(stack.getStackName()).getTemplate();
        cfnAssetDirectory = synth.getDirectory();
    }

    /**
     * Upload assets (mainly lambda function jars) to S3
     */
    private void uploadAssets() {
        Map<String, Asset> assets = findAssets();
        assets.forEach((objectKey, asset) ->
        {
            if (!asset.assetPath.endsWith(".jar")) {
                return;
            }
            ListObjectsV2Response objects =
                    s3.listObjectsV2(ListObjectsV2Request.builder().bucket(asset.bucketName).build());
            if (objects.contents().stream().anyMatch(o -> o.key().equals(objectKey))) {
                LOG.debug("Asset already exists, skipping");
                return;
            }
            LOG.info("Uploading asset " + objectKey + " to " + asset.bucketName);
            s3.putObject(PutObjectRequest.builder().bucket(asset.bucketName).key(objectKey).build(),
                    Paths.get(cfnAssetDirectory, asset.assetPath));
        });
    }

    /**
     * Reading the cdk assets.json file to retrieve the list of assets to push to S3
     *
     * @return a map of assets
     */
    private Map<String, Asset> findAssets() {
        Map<String, Asset> assets = new HashMap<>();
        try {
            JsonNode jsonNode = JsonConfig.get().getObjectMapper()
                    .readTree(new File(cfnAssetDirectory, stackName + ".assets.json"));
            JsonNode files = jsonNode.get("files");
            files.iterator().forEachRemaining(file ->
            {
                String assetPath = file.get("source").get("path").asText();
                String assetPackaging = file.get("source").get("packaging").asText();
                String bucketName =
                        file.get("destinations").get("current_account-current_region").get("bucketName").asText();
                String objectKey =
                        file.get("destinations").get("current_account-current_region").get("objectKey").asText();
                Asset asset = new Asset(assetPath, assetPackaging, bucketName.replace("${AWS::AccountId}", account)
                        .replace("${AWS::Region}", region.toString()));
                assets.put(objectKey, asset);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return assets;
    }

    public static class Builder {
        public long timeoutInSeconds = 30;
        public String pathToFunction;
        public String testName;
        public AppConfig appConfig;
        private String largeMessagesBucket;
        private String stackName;
        private boolean tracing = false;
        private JavaRuntime runtime;
        private Map<String, String> environmentVariables = new HashMap<>();
        private String idemPotencyTable;
        private String queue;
        private String kinesisStream;
        private String ddbStreamsTableName;

        private Builder() {
            runtime = mapRuntimeVersion("JAVA_VERSION");
        }



        private JavaRuntime mapRuntimeVersion(String environmentVariableName) {
            String javaVersion = System.getenv(environmentVariableName); // must be set in GitHub actions
            JavaRuntime ret = null;
            if (javaVersion == null) {
                throw new IllegalArgumentException(environmentVariableName + " is not set");
            }
            if (javaVersion.startsWith("8")) {
                ret = JavaRuntime.JAVA8AL2;
            } else if (javaVersion.startsWith("11")) {
                ret = JavaRuntime.JAVA11;
            } else if (javaVersion.startsWith("17")) {
                ret = JavaRuntime.JAVA17;
            } else if (javaVersion.startsWith("21")) {
                ret = JavaRuntime.JAVA21;
            } else {
                throw new IllegalArgumentException("Unsupported Java version " + javaVersion);
            }
            LOG.debug("Java Version set to {}, using runtime variable {}", ret, javaVersion);
            return ret;
        }

        public Infrastructure build() {
            Objects.requireNonNull(testName, "testName must not be null");

            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            stackName = testName + "-" + uuid;

            Objects.requireNonNull(pathToFunction, "pathToFunction must not be null");
            return new Infrastructure(this);
        }

        public Builder testName(String testName) {
            this.testName = testName;
            return this;
        }

        public Builder pathToFunction(String pathToFunction) {
            this.pathToFunction = pathToFunction;
            return this;
        }

        public Builder tracing(boolean tracing) {
            this.tracing = tracing;
            return this;
        }

        public Builder idempotencyTable(String tableName) {
            this.idemPotencyTable = tableName;
            return this;
        }

        public Builder appConfig(AppConfig app) {
            this.appConfig = app;
            return this;
        }

        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public Builder timeoutInSeconds(long timeoutInSeconds) {
            this.timeoutInSeconds = timeoutInSeconds;
            return this;
        }

        public Builder queue(String queue) {
            this.queue = queue;
            return this;
        }

        public Builder kinesisStream(String stream) {
            this.kinesisStream = stream;
            return this;
        }

        public Builder ddbStreamsTableName(String tableName) {
            this.ddbStreamsTableName = tableName;
            return this;
        }

        public Builder largeMessagesBucket(String largeMessagesBucket) {
            this.largeMessagesBucket = largeMessagesBucket;
            return this;
        }
    }

    private static class Asset {
        private final String assetPath;
        private final String assetPackaging;
        private final String bucketName;

        Asset(String assetPath, String assetPackaging, String bucketName) {
            this.assetPath = assetPath;
            this.assetPackaging = assetPackaging;
            this.bucketName = bucketName;
        }

        @Override
        public String toString() {
            return "Asset{" +
                    "assetPath='" + assetPath + '\'' +
                    ", assetPackaging='" + assetPackaging + '\'' +
                    ", bucketName='" + bucketName + '\'' +
                    '}';
        }
    }
}
