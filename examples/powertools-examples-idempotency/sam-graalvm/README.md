# Powertools for AWS Lambda (Java) - Idempotency Example with SAM on GraalVM

This project contains an example of a Lambda function using the idempotency module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/idempotency/).

The example exposes a HTTP POST endpoint. When the user sends the address of a webpage to it, the endpoint fetches the contents of the URL and returns them to the user.

Have a look at [App.java](src/main/java/helloworld/App.java) for the full details.

## Build the sample application

> [!NOTE]
> Building AWS Lambda packages on macOS (ARM64/Intel) for deployment on AWS Lambda (Linux x86_64 or ARM64) will result in incompatible binary dependencies that cause import errors at runtime.

Choose the appropriate build method based on your operating system:

### Build locally using Docker

Recommended for macOS and Windows users: Cross-compile using Docker to match target platform of Lambda:

```shell
docker build --platform linux/amd64 . -t powertools-examples-idempotency-sam-graalvm
docker run --platform linux/amd64 -it -v `pwd`:`pwd` -w `pwd` -v ~/.m2:/root/.m2 powertools-examples-idempotency-sam-graalvm mvn clean -Pnative-image package -DskipTests
sam build --use-container --build-image powertools-examples-idempotency-sam-graalvm
```

**Note**: The Docker run command mounts your local Maven cache (`~/.m2`) and builds the native binary with SNAPSHOT support, then SAM packages the pre-built binary.

### Build on native OS

For Linux users with GraalVM installed:

```shell
export JAVA_HOME=<path to GraalVM>
mvn clean -Pnative-image package -DskipTests
sam build
```

## Deploy the sample application

```shell
sam deploy
```

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting started with SAM in [the examples directory](../../README.md)

## Test the application

```bash
curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/helloidem/ -H "Content-Type: application/json" -d '{"address": "https://checkip.amazonaws.com"}'
```

this should return the contents of the webpage, for instance:

```json
{ "message": "hello world", "location": "123.123.123.1" }
```

- First call will execute the handleRequest normally, and store the response in the idempotency table (Look into DynamoDB)
- Second call (and next ones) will retrieve from the cache (if cache is enabled, which is by default) or from the store, the handler won't be called. Until the expiration happens (by default 1 hour).

Check out [App.java](src/main/java/helloworld/App.java) to see how it works!
