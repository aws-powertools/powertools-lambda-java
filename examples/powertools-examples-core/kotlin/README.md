#  Powertools for AWS Lambda (Kotlin) - Core Utilities Example

This project demonstrates the Lambda for Powertools Kotlin module deployed using [Serverless Application Model](https://aws.amazon.com/serverless/sam/) with
[Gradle](https://gradle.org/) running the build. This example is configured for Java 1.8 only; in order to use a newer version, check out the Gradle 
configuration guide [in the main project README](../../../README.md).

You can also use `sam init` to create a new Gradle-powered Powertools application - choose to use the **AWS Quick Start Templates**,
and then **Hello World Example with Powertools for AWS Lambda**, **Java 17** runtime, and finally **gradle**.

For general information on the deployed example itself, you can refer to the parent [README](../README.md)

## Configuration
SAM uses [template.yaml](template.yaml) to define the application's AWS resources.
This file defines the Lambda function to be deployed as well as API Gateway for it.

The build of the project is managed by Gradle, and configured in [build.gradle.kts](build.gradle.kts)
. 

## Deploy the sample application
To get started, you can use the included template with SAM to run the build and deploy to your AWS environment:

```bash
sam build && sam deploy --guided
```

Once this is done to deploy the example, check out the instructions for getting started with SAM in 
[the examples directory](../../README.md)

## Additional notes

You can watch the trace information or log information using the SAM CLI:
```bash
# Tail the logs
sam logs --tail $MY_STACK

# Tail the traces
sam traces --tail
```