# Powertools for AWS Lambda (Java) - CloudFormation Custom Resource Example with SAM on GraalVM

This project contains an example of a Lambda function using the CloudFormation module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/custom_resources/).

In this example you pass in a bucket name as a parameter and upon CloudFormation events a call is made to a lambda. That lambda attempts to create the bucket on CREATE events, create a new bucket if the name changes with an UPDATE event and delete the bucket upon DELETE events.

Have a look at [App.java](../../src/main/java/helloworld/App.java) for the full details.

## Build the sample application

> [!NOTE]
> Building AWS Lambda packages on macOS (ARM64/Intel) for deployment on AWS Lambda (Linux x86_64 or ARM64) will result in incompatible binary dependencies that cause import errors at runtime.

Choose the appropriate build method based on your operating system:

### Build locally using Docker

Recommended for macOS and Windows users: Cross-compile using Docker to match target platform of Lambda:

```shell
docker build --platform linux/amd64 . -t powertools-examples-cloudformation-sam-graalvm
docker run --platform linux/amd64 -it -v `pwd`/../..:`pwd`/../.. -w `pwd`/../.. -v ~/.m2:/root/.m2 powertools-examples-cloudformation-sam-graalvm mvn clean -Pnative-image package -DskipTests
sam build --use-container --build-image powertools-examples-cloudformation-sam-graalvm
```

**Note**: The Docker run command mounts your local Maven cache (`~/.m2`) and builds the native binary with SNAPSHOT support, then SAM packages the pre-built binary.

### Build on native OS

For Linux users with GraalVM installed:

```shell
export JAVA_HOME=<path to GraalVM>
cd ../..
mvn clean -Pnative-image package -DskipTests
cd infra/sam-graalvm
sam build
```

## Deploy the sample application

```shell
sam deploy --guided --parameter-overrides BucketNameParam=my-unique-bucket-2.6.0718
```

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting started with SAM in [the examples directory](../../../README.md)

## Test the application

The CloudFormation custom resource will be triggered automatically during stack deployment. You can monitor the Lambda function execution in CloudWatch Logs to see the custom resource handling CREATE, UPDATE, and DELETE events for the S3 bucket.

Check out [App.java](../../src/main/java/helloworld/App.java) to see how it works!