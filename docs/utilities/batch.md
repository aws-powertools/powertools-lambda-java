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
    While this utility lowers the chance of processing messages more than once, but it is not guaranteed. 
    We recommend implementing processing logic in an idempotent manner wherever possible, for instance,
    by taking advantage of [the idempotency module](idempotency.md).
    More details on how Lambda works with SQS can be found in the [AWS documentation](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html)

## Install

We simply add `powertools-batch` to our build dependencies. Note - if you are using other Powertools
modules that require code-weaving, you will need to configure that also. Batch does not.

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
and **(2)** return [a specific response](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html#services-sqs-batchfailurereporting){target="_blank" rel="nofollow"}
to report which records failed to be processed.

You use your preferred deployment framework to set the correct configuration while this utility handles the correct response to be returned.

A complete [Serverless Application Model](https://aws.amazon.com/serverless/sam/) example can be found
[here](https://github.com/aws-powertools/powertools-lambda-java/tree/main/examples/powertools-examples-batch) covering
all of the batch sources.



!!! note "You do not need any additional IAM permissions to use this utility, except for what each event source requires."

## Processing messages from SQS

=== "App.java" 
    
    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
    import com.amazonaws.services.lambda.runtime.events.SQSEvent;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;
    import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
    import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
    
    public class SqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
            private final static Logger LOGGER = LogManager.getLogger(SqsBatchHandler.class);
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
            LOGGER.info("Processing product " + p);
        }
    
    }
    ```

=== "Product.java"
    
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

## Processing messages from Kinesis Streams

=== "App.java"
    
    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
    import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;
    import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
    import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
    
    public class KinesisBatchHandler implements RequestHandler<KinesisEvent, StreamsEventResponse> {
    
        private final static Logger LOGGER = LogManager.getLogger(org.demo.batch.sqs.SqsBatchHandler.class);
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
            LOGGER.info("Processing product " + p);
        }
    
    }
    ```

=== "Product.java"
    
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


## Processing messages from DynamoDB Streams

=== "App.java"
    
    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
    import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;
    import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
    import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;
    
    public class DynamoDBStreamBatchHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse> {
    
        private final static Logger LOGGER = LogManager.getLogger(DynamoDBStreamBatchHandler.class);
        private final BatchMessageHandler<DynamodbEvent, StreamsEventResponse> handler;
    
        public DynamoDBStreamBatchHandler() {
            handler = new BatchMessageHandlerBuilder()
                    .withDynamoDbBatchHandler()
                    .buildWithRawMessageHandler(this::processMessage);
        }
    
        private void processMessage(DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord, Context context) {
            LOGGER.info("Processing DynamoDB Stream Record" + dynamodbStreamRecord);
        }
    
        @Override
        public StreamsEventResponse handleRequest(DynamodbEvent ddbEvent, Context context) {
            return handler.processBatch(ddbEvent, context);
        }
    
    
    }
    ```