                                         #  Powertools for AWS Lambda (Java) - Batch Example

This project contains examples of Lambda function using the batch processing module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the
[documentation](https://docs.powertools.aws.dev/lambda-java/utilities/batch/).

Three different examples and SAM deployments are included, covering each of the batch sources:

* [SQS](src/main/java/org/demo/batch/sqs) - SQS batch processing
* [Kinesis Streams](src/main/java/org/demo/batch/kinesis) - Kinesis Streams batch processing
* [DynamoDB Streams](src/main/java/org/demo/batch/dynamo) - DynamoDB Streams batch processing

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md)

This sample contains three different deployments, depending on which batch processor you'd like to use, you can
change to the subdirectory containing the example SAM template, and deploy. For instance, for the SQS batch
deployment:
```bash
cd deploy/sqs
sam build
sam deploy --guided
```

## Test the application

Each of the examples uses a Lambda scheduled every 5 minutes to push a batch, and a separate lambda to read it. To 
see this in action, we can simply tail the logs of our stack:

```bash
sam logs --tail $STACK_NAME
```