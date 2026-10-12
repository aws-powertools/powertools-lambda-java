# Powertools for AWS Lambda (Java) - Large Messages Example

This project contains an example of a Lambda function using the **Large Messages** module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.aws.amazon.com/powertools/java/latest/utilities/large_messages/).

The example demonstrates an SQS listener that processes messages using the `LargeMessages` functional utility. It handles the retrieval of large payloads offloaded to S3 automatically.

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md).

## Test the application

Since this function is triggered by an SQS Queue, you can test it by sending a message to the queue created by the SAM template.

1. **Find your Queue URL:**
   Run the following command (replacing `LargeMessageExample` with the name of your deployed stack):
   ```bash
   aws cloudformation describe-stacks --stack-name LargeMessageExample --query "Stacks[0].Outputs[?OutputKey=='QueueURL'].OutputValue" --output text
    ```
2. **Send a Test Message:**
   Note: To test the actual "Large Message" functionality (payload offloading), you would typically use the SQS Extended Client in a producer application. However, you can verify the Lambda trigger with a standard message:
   ```bash
   aws sqs send-message --queue-url [YOUR_QUEUE_URL] --message-body '{"message": "Hello from CLI"}'
    ```
3. **Verify Logs:**
   Go to AWS CloudWatch Logs and check the Log Group for your function. You should see the processed message logged by the application.

### Run the Large Message Producer

To test the handling of large messages involving S3 offloading, verify you have the SQS Queue URL and the S3 Bucket name created by the stack.

1. **Build the producer tool:**
    ```bash
    cd examples/powertools-examples-large-messages/tools
    mvn compile
    ```

2. **Run the producer:**
    ```bash
    mvn exec:java -Dexec.args="<SQS_QUEUE_URL> <S3_BUCKET_NAME>"
    ```
    Replace `<SQS_QUEUE_URL>` and `<S3_BUCKET_NAME>` with the output values from the deployment.