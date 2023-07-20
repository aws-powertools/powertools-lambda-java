#  Powertools for AWS Lambda (Java) - SQS Batch Processing Example

This project contains an example of Lambda function using the batch processing utilities module of Powertools for AWS Lambda (Java). 
For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda/java/utilities/batch/).

The project contains two functions: 

* [SqsMessageSender](src/main/java/org/demo/sqs/SqsMessageSender.java)  - Sends a set of messages to an SQS queue.
This function is triggered every 5 minutes by an EventBridge schedule rule.
* [SqsPoller](src/main/java/org/demo/sqs/SqsPoller.java) - Listens to the same queue, processing items off in batches

The poller intentionally fails intermittently processing messages to demonstrate the replay behaviour of the batch
module:

<details>
<summary>
<b>SqsPoller.java</b>
</summary>
[SqsPoller.java:43](src/main/java/org/demo/sqs/SqsPoller.java)

```java
 public String process(SQSMessage message) {
            log.info("Processing message with id {}", message.getMessageId());

            int nextInt = random.nextInt(100);

            if(nextInt <= 10) {
                log.info("Randomly picked message with id {} as business validation failure.", message.getMessageId());
                throw new IllegalArgumentException("Failed business validation. No point of retrying. Move me to DLQ." + message.getMessageId());
            }

            if(nextInt > 90) {
                log.info("Randomly picked message with id {} as intermittent failure.", message.getMessageId());
                throw new RuntimeException("Failed due to intermittent issue. Will be sent back for retry." + message.getMessageId());
            }

            return "Success";
        }
```

</details>

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md)

## Test the application

As the test is pushing through a batch every 5 minutes, we can simply watch the logs to see the batches being processed:

```bash
 sam logs --tail --stack-name $MY_STACK    
```

As the handler intentionally introduces intermittent failures, we should expect to see error messages too!
