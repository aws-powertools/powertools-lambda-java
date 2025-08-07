# Powertools for AWS Lambda (Java) - Core Utilities Example with Bazel

This project demonstrates the Lambda for Powertools Java module deployed using [Serverless Application Model](https://aws.amazon.com/serverless/sam/) and built with Bazel.

For general information on the deployed example itself, you can refer to the parent [README](../README.md)

## Configuration

SAM uses [template.yaml](template.yaml) to define the application's AWS resources. This file defines the Lambda function to be deployed as well as API Gateway for it.

## Deploy the sample application

To deploy the example, check out the instructions for getting started with SAM in [the examples directory](../../README.md)

## Build and deploy

```bash
# Build the application
bazel build //:powertools_sam_deploy.jar

# Deploy the application
sam deploy --guided
```

## Local testing

```bash
# Build the application
bazel build //:powertools_sam_deploy.jar

# Test a single function locally
sam local invoke HelloWorldFunction --event events/event.json

# Start the local API
sam local start-api

# Test the API endpoints
curl http://127.0.0.1:3000/hello
curl http://127.0.0.1:3000/hellostream
```

## Additional notes

You can watch the trace information or log information using the SAM CLI:

```bash
# Tail the logs
sam logs --tail $MY_STACK

# Tail the traces
sam traces --tail
```

### Pinning Maven versions

To ensure reproducible builds, you can pin Maven dependency versions:

```bash
# Generate lock file for reproducible builds
bazel run @maven//:pin
```

This creates `maven_install.json` which locks dependency versions and should be committed to version control.
