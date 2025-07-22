---
title: Batch Processing
description: Utility
---

The batch processing utility provides a way to handle partial failures when processing batches of messages from SQS queues,
SQS FIFO queues, Kinesis Streams, or DynamoDB Streams.

```mermaid
stateDiagram-v2
    direction LR
    BatchSource: Amazon SQS <br/><br/> Amazon Kinesis Data Streams <br/><br/> Amazon DynamoDB Streams <br/><br/>
    LambdaInit: Lambda invocation
    BatchProcessor: Batch Processor
    RecordHandler: Record Handler function
    YourLogic: Your logic to process each batch item
    LambdaResponse: Lambda response
    BatchSource --> LambdaInit
    LambdaInit --> BatchProcessor
    BatchProcessor --> RecordHandler
    state BatchProcessor {
        [*] --> RecordHandler: Your function
        RecordHandler --> YourLogic
    }
    RecordHandler --> BatchProcessor: Collect results
    BatchProcessor --> LambdaResponse: Report items that failed processing
```

**Key Features**

* Reports batch item failures to reduce number of retries for a record upon errors
* Simple interface to process each batch record
* Parallel processing of batches
* Integrates with Java Events library and the deserialization module 
* Build your own batch processor by extending primitives

**Background**

When using SQS, Kinesis Data Streams, or DynamoDB Streams as a Lambda event source, your Lambda functions are 
triggered with a batch of messages.
If your function fails to process any message from the batch, the entire batch returns to your queue or stream.
This same batch is then retried until either condition happens first:
**a)** your Lambda function returns a successful response, 
**b)** record reaches maximum retry attempts, or 
**c)** records expire.

```mermaid
journey
  section Conditions
    Successful response: 5: Success
    Maximum retries: 3: Failure
    Records expired: 1: Failure
```

This behavior changes when you enable Report Batch Item Failures feature in your Lambda function event source configuration:

<!-- markdownlint-disable MD013 -->
* [**SQS queues**](#sqs-standard). Only messages reported as failure will return to the queue for a retry, while successful ones will be deleted.
* [**Kinesis data streams**](#kinesis-and-dynamodb-streams) and [**DynamoDB streams**](#kinesis-and-dynamodb-streams).
Single reported failure will use its sequence number as the stream checkpoint. 
Multiple reported failures will use the lowest sequence number as checkpoint.

With this utility, batch records are processed individually – only messages that failed to be processed 
return to the queue or stream for a further retry. You simply build a `BatchProcessor` in your handler,
and return its response from the handler's `processMessage` implementation. Exceptions are handled 
internally and an appropriate partial response for the message source is returned to Lambda for you.

!!! warning
    While this utility lowers the chance of processing messages more than once, it is still not guaranteed. 
    We recommend implementing processing logic in an idempotent manner wherever possible, for instance,
    by taking advantage of [the idempotency module](idempotency.md).
    More details on how Lambda works with SQS can be found in the [AWS documentation](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html)

## Install

We simply add `powertools-batch` to our build dependencies. Note - if you are using other Powertools
modules that require code-weaving, such as `powertools-core`, you will need to configure that also.

=== "Maven"

    ```xml
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-batch</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        ...
    </dependencies>
    ```

=== "Gradle"

    ```groovy
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            implementation 'software.amazon.lambda:powertools-batch:{{ powertools.version }}'
        }
    ```
## Getting Started

For this feature to work, you need to **(1)** configure your Lambda function event source to use `ReportBatchItemFailures`,
and **(2)** return a specific response to report which records failed to be processed. 

You can use your preferred deployment framework to set the correct configuration while this utility,
while the `powertools-batch` module handles generating the response, which simply needs to be returned as the result of
your Lambda handler.

A complete [Serverless Application Model](https://aws.amazon.com/serverless/sam/) example can be found [here](https://github.com/aws-powertools/powertools-lambda-java/tree/main/examples/powertools-examples-batch) covering all the batch sources.

For more information on configuring `ReportBatchItemFailures`, see the details for [SQS](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#services-sqs-batchfailurereporting), [Kinesis](https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html#services-kinesis-batchfailurereporting), and [DynamoDB Streams](https://docs.aws.amazon.com/lambda/latest/dg/with-ddb.html#services-ddb-batchfailurereporting). 


!!! note "You do not need any additional IAM permissions to use this utility, except for what each event source requires."

### Processing messages from SQS

=== "SQSBatchHandler" 
    
    ```java hl_lines="10 13-15 20 25"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
    import com.amazonaws.services.lambda.runtime.events.SQSEvent;
    import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
    import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
    
    public class SqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

        private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;
    
        public SqsBatchHandler() {
            handler = new BatchMessageHandlerBuilder()
                    .withSqsBatchHandler()
                    .buildWithMessageHandler(this::processMessage, Product.class);
        }
    
        @Override
        public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
            return handler.processBatch(sqsEvent, context);
        }

        private void processMessage(Product p, Context c) {
            // Process the product
        }
    }
    ```

=== "SQS Product"
    
    ```java
    public class Product {
        private long id;
    
        private String name;
    
        private double price;
    
        public Product() {
        }
    
        public Product(long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    
        public long getId() {
            return id;
        }
    
        public void setId(long id) {
            this.id = id;
        }
    
        public String getName() {
            return name;
        }
    
        public void setName(String name) {
            this.name = name;
        }
    
        public double getPrice() {
            return price;
        }
    
        public void setPrice(double price) {
            this.price = price;
        }
    }
    ``` 

=== "SQS Example Event"

    ```json
        {
            "Records": [
            {
                "messageId": "d9144555-9a4f-4ec3-99a0-34ce359b4b54",
                "receiptHandle": "13e7f7851d2eaa5c01f208ebadbf1e72==",
                "body": "{\n  \"id\": 1234,\n  \"name\": \"product\",\n  \"price\": 42\n}",
                "attributes": {
                    "ApproximateReceiveCount": "1",
                    "SentTimestamp": "1601975706495",
                    "SenderId": "AROAIFU437PVZ5L2J53F5",
                    "ApproximateFirstReceiveTimestamp": "1601975706499"
                },
                "messageAttributes": {
                },
                "md5OfBody": "13e7f7851d2eaa5c01f208ebadbf1e72",
                "eventSource": "aws:sqs",
                "eventSourceARN": "arn:aws:sqs:eu-central-1:123456789012:TestLambda",
                "awsRegion": "eu-central-1"
            },
            {
                "messageId": "e9144555-9a4f-4ec3-99a0-34ce359b4b54",
                "receiptHandle": "13e7f7851d2eaa5c01f208ebadbf1e72==",
                "body": "{\n  \"id\": 12345,\n  \"name\": \"product5\",\n  \"price\": 45\n}",
                "attributes": {
                    "ApproximateReceiveCount": "1",
                    "SentTimestamp": "1601975706495",
                    "SenderId": "AROAIFU437PVZ5L2J53F5",
                    "ApproximateFirstReceiveTimestamp": "1601975706499"
                },
                "messageAttributes": {
                },
                "md5OfBody": "13e7f7851d2eaa5c01f208ebadbf1e72",
                "eventSource": "aws:sqs",
                "eventSourceARN": "arn:aws:sqs:eu-central-1:123456789012:TestLambda",
                "awsRegion": "eu-central-1"
            }]
        }
    ```

### Processing messages from Kinesis Streams

=== "KinesisBatchHandler"
    
    ```java  hl_lines="10 13-15 20 24"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
    import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
    import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
    import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
    
    public class KinesisBatchHandler implements RequestHandler<KinesisEvent, StreamsEventResponse> {
    
        private final BatchMessageHandler<KinesisEvent, StreamsEventResponse> handler;
    
        public KinesisBatchHandler() {
            handler = new BatchMessageHandlerBuilder()
                    .withKinesisBatchHandler()
                    .buildWithMessageHandler(this::processMessage, Product.class);
        }
    
        @Override
        public StreamsEventResponse handleRequest(KinesisEvent kinesisEvent, Context context) {
            return handler.processBatch(kinesisEvent, context);
        }
    
        private void processMessage(Product p, Context c) {
            // process the product
        }
    }
    ```

=== "Kinesis Product"
    
    ```java
    public class Product {
        private long id;
    
        private String name;
    
        private double price;
    
        public Product() {
        }
    
        public Product(long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    
        public long getId() {
            return id;
        }
    
        public void setId(long id) {
            this.id = id;
        }
    
        public String getName() {
            return name;
        }
    
        public void setName(String name) {
            this.name = name;
        }
    
        public double getPrice() {
            return price;
        }
    
        public void setPrice(double price) {
            this.price = price;
        }
    }
    ``` 

=== "Kinesis Example Event"

    ```json 
        {
          "Records": [
            {
              "kinesis": {
                "partitionKey": "partitionKey-03",
                "kinesisSchemaVersion": "1.0",
                "data": "eyJpZCI6MTIzNCwgIm5hbWUiOiJwcm9kdWN0IiwgInByaWNlIjo0Mn0=",
                "sequenceNumber": "49545115243490985018280067714973144582180062593244200961",
                "approximateArrivalTimestamp": 1428537600,
                "encryptionType": "NONE"
              },
              "eventSource": "aws:kinesis",
              "eventID": "shardId-000000000000:49545115243490985018280067714973144582180062593244200961",
              "invokeIdentityArn": "arn:aws:iam::EXAMPLE",
              "eventVersion": "1.0",
              "eventName": "aws:kinesis:record",
              "eventSourceARN": "arn:aws:kinesis:EXAMPLE",
              "awsRegion": "eu-central-1"
            },
            {
              "kinesis": {
                "partitionKey": "partitionKey-03",
                "kinesisSchemaVersion": "1.0",
                "data": "eyJpZCI6MTIzNDUsICJuYW1lIjoicHJvZHVjdDUiLCAicHJpY2UiOjQ1fQ==",
                "sequenceNumber": "49545115243490985018280067714973144582180062593244200962",
                "approximateArrivalTimestamp": 1428537600,
                "encryptionType": "NONE"
              },
              "eventSource": "aws:kinesis",
              "eventID": "shardId-000000000000:49545115243490985018280067714973144582180062593244200961",
              "invokeIdentityArn": "arn:aws:iam::EXAMPLE",
              "eventVersion": "1.0",
              "eventName": "aws:kinesis:record",
              "eventSourceARN": "arn:aws:kinesis:EXAMPLE",
              "awsRegion": "eu-central-1"
            }
          ]
        }
    ```
### Processing messages from DynamoDB Streams

=== "DynamoDBStreamBatchHandler"
    
    ```java  hl_lines="10 13-15 20 24"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
    import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
    import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
    import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
    
    public class DynamoDBStreamBatchHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse> {
    
        private final BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler;
    
        public DynamoDBStreamBatchHandler() {
            handler = new BatchMessageHandlerBuilder()
                    .withDynamoDbBatchHandler()
                    .buildWithRawMessageHandler(this::processMessage);
        }
        
        @Override
        public StreamsEventResponse handleRequest(DynamodbEvent ddbEvent, Context context) {
            return handler.processBatch(ddbEvent, context);
        }

        private void processMessage(DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord, Context context) {
            // Process the change record
        }
    }
    ```

=== "DynamoDB Example Event"

    ```json 
        {
          "Records": [
            {
              "eventID": "c4ca4238a0b923820dcc509a6f75849b",
              "eventName": "INSERT",
              "eventVersion": "1.1",
              "eventSource": "aws:dynamodb",
              "awsRegion": "eu-central-1",
              "dynamodb": {
                "Keys": {
                  "Id": {
                    "N": "101"
                  }
                },
                "NewImage": {
                  "Message": {
                    "S": "New item!"
                  },
                  "Id": {
                    "N": "101"
                  }
                },
                "ApproximateCreationDateTime": 1428537600,
                "SequenceNumber": "4421584500000000017450439091",
                "SizeBytes": 26,
                "StreamViewType": "NEW_AND_OLD_IMAGES"
              },
              "eventSourceARN": "arn:aws:dynamodb:eu-central-1:123456789012:table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899",
              "userIdentity": {
                "principalId": "dynamodb.amazonaws.com",
                "type": "Service"
              }
            },
            {
              "eventID": "c81e728d9d4c2f636f067f89cc14862c",
              "eventName": "MODIFY",
              "eventVersion": "1.1",
              "eventSource": "aws:dynamodb",
              "awsRegion": "eu-central-1",
              "dynamodb": {
                "Keys": {
                  "Id": {
                    "N": "101"
                  }
                },
                "NewImage": {
                  "Message": {
                    "S": "This item has changed"
                  },
                  "Id": {
                    "N": "101"
                  }
                },
                "OldImage": {
                  "Message": {
                    "S": "New item!"
                  },
                  "Id": {
                    "N": "101"
                  }
                },
                "ApproximateCreationDateTime": 1428537600,
                "SequenceNumber": "4421584500000000017450439092",
                "SizeBytes": 59,
                "StreamViewType": "NEW_AND_OLD_IMAGES"
              },
              "eventSourceARN": "arn:aws:dynamodb:eu-central-1:123456789012:table/ExampleTableWithStream/stream/2015-06-27T00:48:05.899"
            }
          ]
        }
    ```

## Parallel processing
You can choose to process batch items in parallel using the `BatchMessageHandler#processBatchInParallel()` 
instead of `BatchMessageHandler#processBatch()`. Partial batch failure works the same way but items are processed
in parallel rather than sequentially. 

This feature is available for SQS, Kinesis and DynamoDB Streams but cannot be 
used with SQS FIFO. In that case, an `UnsupportedOperationException` is thrown.

!!! warning
    Note that parallel processing is not always better than sequential processing, 
    and you should benchmark your code to determine the best approach for your use case. 

!!! info
    To get more threads available (more vCPUs), you need to increase the amount of memory allocated to your Lambda function.
    While it is possible to increase the number of threads using Java options or custom thread pools, 
    in most cases the defaults work well, and changing them is more likely to decrease performance 
    (see [here](https://www.baeldung.com/java-when-to-use-parallel-stream#fork-join-framework)
    and [here](https://dzone.com/articles/be-aware-of-forkjoinpoolcommonpool)). 
    In situations where this may be useful - such as performing IO-bound work in parallel - make sure to measure before and after!


=== "Example with SQS"

    ```java hl_lines="13"
    public class SqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

        private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;
    
        public SqsBatchHandler() {
            handler = new BatchMessageHandlerBuilder()
                    .withSqsBatchHandler()
                    .buildWithMessageHandler(this::processMessage, Product.class);
        }
    
        @Override
        public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
            return handler.processBatchInParallel(sqsEvent, context);
        }
    
        private void processMessage(Product p, Context c) {
            // Process the product
        }
    }
    ```
=== "Example with SQS (using custom executor)"

    ```java hl_lines="4 10 15"
    public class SqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

        private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;
        private final ExecutorService executor;
    
        public SqsBatchHandler() {
            handler = new BatchMessageHandlerBuilder()
                    .withSqsBatchHandler()
                    .buildWithMessageHandler(this::processMessage, Product.class);
            executor = Executors.newFixedThreadPool(2);
        }
    
        @Override
        public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
            return handler.processBatchInParallel(sqsEvent, context, executor);
        }
    
        private void processMessage(Product p, Context c) {
            // Process the product
        }
    }
    ```


## Handling Messages

### Raw message and deserialized message handlers
You must provide either a raw message handler, or a deserialized message handler. The raw message handler receives
the envelope record type relevant for the particular event source - for instance, the SQS event source provides
[SQSMessage](https://javadoc.io/doc/com.amazonaws/aws-lambda-java-events/2.2.2/com/amazonaws/services/lambda/runtime/events/SQSEvent.html)
instances. The deserialized message handler extracts the body from this envelope, and deserializes it to a user-defined
type. Note that deserialized message handlers are not relevant for the DynamoDB provider, as the format of the inner 
message is fixed by DynamoDB.

In general, the deserialized message handler should be used unless you need access to information on the envelope.

=== "Raw Message Handler"

    ```java hl_lines="4 7"
    public void setup() {
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithRawMessageHandler(this::processRawMessage);
    }

    private void processRawMessage(SQSEvent.SQSMessage sqsMessage) {
        // Do something with the raw message
    }
    
    ```

=== "Deserialized Message Handler" 

    ```java hl_lines="4 7"
    public void setup() {
        BatchMessageHandler<SQSEvent, SQSBatchResponse> handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWitMessageHandler(this::processRawMessage, Product.class);
    }

    private void processMessage(Product product) {
        // Do something with the deserialized message
    }
    
    ```

### Success and failure handlers

You can register a success or failure handler which will be invoked as each message is processed by the batch
module. This may be useful for reporting - for instance, writing metrics or logging failures. 

These handlers are optional. Batch failures are handled by the module regardless of whether or not you 
provide a custom failure handler. 

Handlers can be provided when building the batch processor and are available for all event sources.
For instance for DynamoDB:

```java hl_lines="3 8"
BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler = new BatchMessageHandlerBuilder()
    .withDynamoDbBatchHandler()
    .withSuccessHandler((m) -> {
        // Success handler receives the raw message
        LOGGER.info("Message with sequenceNumber {} was successfully processed",
            m.getDynamodb().getSequenceNumber());
    })
    .withFailureHandler((m, e) -> {
        // Failure handler receives the raw message and the exception thrown.
        LOGGER.info("Message with sequenceNumber {} failed to be processed: {}"
        , e.getDynamodb().getSequenceNumber(), e);
    })
    .buildWithMessageHander(this::processMessage);
```

!!! info
    If the success handler throws an exception, the item it is processing will be marked as failed by the
    batch processor.
    If the failure handler throws, the batch processing will continue; the item it is processing has
    already been marked as failed.


### Lambda Context 

Both raw and deserialized message handlers can choose to take the Lambda context as an argument if they
need it, or not:

```java
    public class ClassWithHandlers {

        private void processMessage(Product product) {
            // Do something with the raw message
        }
    
        private void processMessageWithContext(Product product, Context context) {
            // Do something with the raw message and the lambda Context
        }
    }
```
