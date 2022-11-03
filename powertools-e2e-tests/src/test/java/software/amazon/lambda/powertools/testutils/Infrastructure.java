package software.amazon.lambda.powertools.testutils;

import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.Status;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.*;
import software.amazon.awscdk.cxapi.CloudAssembly;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.lambda.powertools.utilities.JsonConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;

import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonList;

public class Infrastructure {
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
    private final LambdaClient lambda;
    private final CloudFormationClient cfn;
    private final CloudWatchClient cloudwatch;
    private final Region region;
    private final String account;
    private final String idempotencyTable;
    private String functionName;
    private Object cfnTemplate;
    private String cfnAssetDirectory;
    private final SdkHttpClient httpClient;

    private Infrastructure(Builder builder) {
        this.stackName = builder.stackName;
        this.tracing = builder.tracing;
        this.envVar = builder.environmentVariables;
        this.runtime = builder.runtime;
        this.timeout = builder.timeoutInSeconds;
        this.pathToFunction = builder.pathToFunction;
        this.idempotencyTable = builder.idemPotencyTable;

        this.app = new App();
        this.stack = createStackWithLambda();

        this.synthesize();

        this.httpClient = UrlConnectionHttpClient.builder().build();
        this.region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
        this.account = StsClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build().getCallerIdentity().account();

        s3 = S3Client.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
        lambda = LambdaClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
        cloudwatch = CloudWatchClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
        cfn = CloudFormationClient.builder()
                .httpClient(httpClient)
                .region(region)
                .build();
    }

    public void deploy() {
        uploadAssets();
        LOG.debug("Deploying '" + stackName + "' on account " + account);
        cfn.createStack(CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(new Yaml().dump(cfnTemplate))
                .timeoutInMinutes(10)
                .onFailure(OnFailure.ROLLBACK)
                .capabilities(Capability.CAPABILITY_IAM)
                .build());
        WaiterResponse<DescribeStacksResponse> waiterResponse = cfn.waiter().waitUntilStackCreateComplete(DescribeStacksRequest.builder().stackName(stackName).build());
        if (waiterResponse.matched().response().isPresent()) {
            LOG.debug("Stack " + waiterResponse.matched().response().get().stacks().get(0).stackName() + " successfully deployed");
        } else {
            throw new RuntimeException("Failed to create stack");
        }
    }

    public void destroy() {
        LOG.debug("Deleting '" + stackName + "' on account " + account);
        cfn.deleteStack(DeleteStackRequest.builder().stackName(stackName).build());
    }

    public InvocationResult invokeFunction(String input) {
        SdkBytes payload = SdkBytes.fromUtf8String(input);

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(payload)
                .logType(LogType.TAIL)
                .build();

        Instant start = Instant.now(Clock.systemUTC()).truncatedTo(MINUTES);
        InvokeResponse response = lambda.invoke(request);
        Instant end = start.plus(1, MINUTES);
        return new InvocationResult(response, start, end);
    }

    public List<Double> getMetrics(Instant start, Instant end, int period, String namespace, String metricName, Map<String, String> dimensions) {
        List<Dimension> dimensionsList = new ArrayList<>();
        if (dimensions != null)
            dimensions.forEach((key, value) -> dimensionsList.add(Dimension.builder().name(key).value(value).build()));

        Callable<List<Double>> callable = () -> {
            LOG.debug("Get Metrics for namespace {}, start {}, end {}, metric {}, dimensions {}", namespace, start, end, metricName, dimensionsList);
            GetMetricDataResponse metricData = cloudwatch.getMetricData(GetMetricDataRequest.builder()
                    .startTime(start)
                    .endTime(end)
                    .metricDataQueries(MetricDataQuery.builder()
                            .id(metricName.toLowerCase())
                            .metricStat(MetricStat.builder()
                                    .unit(StandardUnit.COUNT)
                                    .metric(Metric.builder()
                                            .namespace(namespace)
                                            .metricName(metricName)
                                            .dimensions(dimensionsList)
                                            .build())
                                    .period(period)
                                    .stat("Sum")
                                    .build())
                            .returnData(true)
                            .build())
                    .build());
            List<Double> values = metricData.metricDataResults().get(0).values();
            if (values == null || values.isEmpty()) {
                throw new Exception("No data found for metric " + metricName);
            }
            return values;
        };

        RetryConfig retryConfig = new RetryConfigBuilder()
                .withMaxNumberOfTries(10)
                .retryOnAnyException()
                .withDelayBetweenTries(ofSeconds(2))
                .withRandomExponentialBackoff()
                .build();
        CallExecutor<List<Double>> callExecutor = new CallExecutorBuilder<List<Double>>()
                .config(retryConfig)
                .afterFailedTryListener(s -> {LOG.warn(s.getLastExceptionThatCausedRetry().getMessage() + ", attempts: " + s.getTotalTries());})
                .build();
        Status<List<Double>> status = callExecutor.execute(callable);
        return status.getResult();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFunctionName() {
        return functionName;
    }

    public static class Builder {
        public long timeoutInSeconds = 30;
        public String pathToFunction;
        public String testName;
        private String stackName;
        private boolean tracing = false;
        private JavaRuntime runtime = JavaRuntime.JAVA11;
        private Map<String, String> environmentVariables = new HashMap<>();
        private String idemPotencyTable;

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

        public Builder runtime(JavaRuntime runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder idempotencyTable(String tableName) {
            this.idemPotencyTable = tableName;
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
    }

    private Stack createStackWithLambda() {
        Stack stack = new Stack(app, stackName);
        List<String> packagingInstruction = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd " + pathToFunction +
                        " && timeout -s SIGKILL 5m mvn clean install -ff " +
                        " -Dmaven.test.skip=true " +
                        " -Dmaven.resources.skip=true " +
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
            table.grantReadWriteData(function);
        }

        return stack;
    }

    private void synthesize() {
        CloudAssembly synth = app.synth();
        cfnTemplate = synth.getStackByName(stack.getStackName()).getTemplate();
        cfnAssetDirectory = synth.getDirectory();
    }

    private void uploadAssets() {
        Map<String, Asset> assets = findAssets();
        assets.forEach((objectKey, asset) -> {
            if (!asset.assetPath.endsWith(".jar")) {
                return;
            }
            ListObjectsV2Response objects = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(asset.bucketName).build());
            if (objects.contents().stream().anyMatch(o -> o.key().equals(objectKey))) {
                System.out.println("Asset already exists, skipping");
                return;
            }
            System.out.println("Uploading asset " + objectKey + " to " + asset.bucketName);
            s3.putObject(PutObjectRequest.builder().bucket(asset.bucketName).key(objectKey).build(), Path.of(cfnAssetDirectory, asset.assetPath));
        });
    }

    private Map<String, Asset> findAssets() {
        Map<String, Asset> assets = new HashMap<>();
        try {
            JsonNode jsonNode = JsonConfig.get().getObjectMapper().readTree(new File(cfnAssetDirectory, stackName + ".assets.json"));
            JsonNode files = jsonNode.get("files");
            files.iterator().forEachRemaining(file -> {
                String assetPath = file.get("source").get("path").asText();
                String assetPackaging = file.get("source").get("packaging").asText();
                String bucketName = file.get("destinations").get("current_account-current_region").get("bucketName").asText();
                String objectKey = file.get("destinations").get("current_account-current_region").get("objectKey").asText();
                Asset asset = new Asset(assetPath, assetPackaging, bucketName.replace("${AWS::AccountId}", account).replace("${AWS::Region}", region.toString()));
                assets.put(objectKey, asset);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return assets;
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
