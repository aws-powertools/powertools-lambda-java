#  Powertools for AWS Lambda (Java) - Core Utilities Example with SAM

This project demonstrates the Lambda for Powertools Java module deployed using [Serverless Application Model](https://aws.amazon.com/serverless/sam/).

For general information on project structure similar to all architectures, you can refer to the parent [README](../README.md)

## Configuration
SAM uses [template.yaml](template.yaml) to define the application's AWS resources.
This file defines the Lambda function to be deployed as well as API Gateway for it.

## Deploy the sample application
To deploy the example, check out the instructions for getting
started with SAM in [the examples directory](../../README.md)

## Additional notes

You can watch the trace information or log information using the SAM CLI:
```bash
# Tail the logs
sam logs --tail $MY_STACK

# Tail the traces
sam traces --tail
```