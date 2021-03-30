---
title: SQS Batch Processing
description: Utility
---

The SQS batch processing utility provides a way to handle partial failures when processing batches of messages from SQS.

**Key Features**

* Prevent successfully processed messages from being returned to SQS
* A simple interface for individually processing messages from a batch

**Background**

When using SQS as a Lambda event source mapping, Lambda functions can be triggered with a batch of messages from SQS. 
If your function fails to process any message from the batch, the entire batch returns to your SQS queue, and your 
Lambda function will be triggered with the same batch again. With this utility, messages within a batch will be handled individually - only messages that were not successfully processed
are returned to the queue.

!!! warning
    While this utility lowers the chance of processing messages more than once, it is not guaranteed. We recommend implementing processing logic in an idempotent manner wherever possible.
    More details on how Lambda works with SQS can be found in the [AWS documentation](https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html)

## Install

To install this utility, add the following dependency to your project.

!!! note "Using Java 9 or later?"
    If you are working with lambda function on runtime **Java 9 or later**, please refer **[issue](https://github.com/awslabs/aws-lambda-powertools-java/issues/50)** for a workaround.

=== "Maven"
    ```xml hl_lines="3 4 5 6 7 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-sqs</artifactId>
            <version>1.5.0</version>
        </dependency>
        ...
    </dependencies>
    <!-- configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project -->
    <build>
        <plugins>
            ...
            <plugin>
                 <groupId>org.codehaus.mojo</groupId>
                 <artifactId>aspectj-maven-plugin</artifactId>
                 <version>1.11</version>
                 <configuration>
                     <source>1.8</source>
                     <target>1.8</target>
                     <complianceLevel>1.8</complianceLevel>
                     <aspectLibraries>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-sqs</artifactId>
                         </aspectLibrary>
                     </aspectLibraries>
                 </configuration>
                 <executions>
                     <execution>
                         <goals>
                             <goal>compile</goal>
                         </goals>
                     </execution>
                 </executions>
            </plugin>
            ...
        </plugins>
    </build>
    ```

=== "Gradle"

    ```groovy
     dependencies {
        ...
        implementation 'software.amazon.lambda:powertools-sqs:1.5.0'
        aspectpath 'software.amazon.lambda:powertools-sqs:1.5.0'
    }
    ```

**IAM Permissions**

This utility requires additional permissions to work as expected. Lambda functions using this utility require the `sqs:GetQueueUrl` and `sqs:DeleteMessageBatch` permission.

## Processing messages from SQS

You can use either **[SqsBatch annotation](#sqsbatch-annotation)**, or **[SqsUtils Utility API](#sqsutils-utility-api)** as a fluent API.

Both have nearly the same behaviour when it comes to processing messages from the batch:

* **Entire batch has been successfully processed**, where your Lambda handler returned successfully, we will let SQS delete the batch to optimize your cost
* **Entire Batch has been partially processed successfully**, where exceptions were raised within your `SqsMessageHandler` interface implementation, we will:
    - **1)** Delete successfully processed messages from the queue by directly calling `sqs:DeleteMessageBatch`
    - **2)** Raise `SQSBatchProcessingException` to ensure failed messages return to your SQS queue

The only difference is that **SqsUtils Utility API** will give you access to return from the processed messages if you need. Exception `SQSBatchProcessingException` thrown from the
utility will have access to both successful and failed messaged along with failure exceptions.

## Functional Interface SqsMessageHandler

Both [annotation](#sqsbatch-annotation) and [SqsUtils Utility API](#sqsutils-utility-api) requires an implementation of functional interface `SqsMessageHandler`.

This implementation is responsible for processing each individual message from the batch, and to raise an exception if unable to process any of the messages sent.

**Any non-exception/successful return from your record handler function** will instruct utility to queue up each individual message for deletion.

### SqsBatch annotation

When using this annotation, you need provide a class implementation of `SqsMessageHandler` that will process individual messages from the batch - It should raise an exception if it is unable to process the record.

All records in the batch will be passed to this handler for processing, even if exceptions are thrown - Here's the behaviour after completing the batch:

* **Any successfully processed messages**, we will delete them from the queue via `sqs:DeleteMessageBatch`
* **Any unprocessed messages detected**, we will raise `SQSBatchProcessingException` to ensure failed messages return to your SQS queue

!!! warning
    You will not have access to the **processed messages** within the Lambda Handler - all processing logic will and should be performed by the implemented `#!java SqsMessageHandler#process()` function.

=== "AppSqsEvent.java"

    ```java hl_lines="3"
    public class AppSqsEvent implements RequestHandler<SQSEvent, String> {
        @Override
        @SqsBatch(SampleMessageHandler.class)
        public String handleRequest(SQSEvent input, Context context) {
            return "{\"statusCode\": 200}";
        }
    
        public class SampleMessageHandler implements SqsMessageHandler<Object> {
    
            @Override
            public String process(SQSMessage message) {
                // This will be called for each individual message from a batch
                // It should raise an exception if the message was not processed successfully
                String returnVal = doSomething(message.getBody());
                return returnVal;
            }
        }
    }
    ```

### SqsUtils Utility API

If you require access to the result of processed messages, you can use this utility. The result from calling **`#!java SqsUtils#batchProcessor()`** on the context manager will be a list of all the return values 
from your **`#!java SqsMessageHandler#process()`** function.

You can also use the utility in functional way by providing inline implementation of functional interface **`#!java SqsMessageHandler#process()`**


=== "Utility API"
    
    ```java hl_lines="4"
    public class AppSqsEvent implements RequestHandler<SQSEvent, List<String>> {
        @Override
        public List<String> handleRequest(SQSEvent input, Context context) {
            List<String> returnValues = SqsUtils.batchProcessor(input, SampleMessageHandler.class);
    
            return returnValues;
        }
    
        public class SampleMessageHandler implements SqsMessageHandler<String> {
    
            @Override
            public String process(SQSMessage message) {
                // This will be called for each individual message from a batch
                // It should raise an exception if the message was not processed successfully
                String returnVal = doSomething(message.getBody());
                return returnVal;
            }
        }
    }
    ```

=== "Function implementation"

    ```java hl_lines="5 6 7 8 9 10"
    public class AppSqsEvent implements RequestHandler<SQSEvent, List<String>> {
    
        @Override
        public List<String> handleRequest(SQSEvent input, Context context) {
            List<String> returnValues = SqsUtils.batchProcessor(input, (message) -> {
                // This will be called for each individual message from a batch
                // It should raise an exception if the message was not processed successfully
                String returnVal = doSomething(message.getBody());
                return returnVal;
            });
    
            return returnValues;
        }
    }
    ```

## Passing custom SqsClient

If you need to pass custom SqsClient such as region to the SDK, you can pass your own `SqsClient` to be used by utility either for
**[SqsBatch annotation](#sqsbatch-annotation)**, or **[SqsUtils Utility API](#sqsutils-utility-api)**.

=== "App.java"

    ```java hl_lines="3 4"
    public class AppSqsEvent implements RequestHandler<SQSEvent, List<String>> {
        static {
            SqsUtils.overrideSqsClient(SqsClient.builder()
                    .build());
        }
    
        @Override
        public List<String> handleRequest(SQSEvent input, Context context) {
            List<String> returnValues = SqsUtils.batchProcessor(input, SampleMessageHandler.class);
    
            return returnValues;
        }
    
        public class SampleMessageHandler implements SqsMessageHandler<String> {
    
            @Override
            public String process(SQSMessage message) {
                // This will be called for each individual message from a batch
                // It should raise an exception if the message was not processed successfully
                String returnVal = doSomething(message.getBody());
                return returnVal;
            }
        }
    }
    ```

## Suppressing exceptions

If you want to disable the default behavior where `SQSBatchProcessingException` is raised if there are any exception, you can pass the `suppressException` boolean argument.

=== "Within SqsBatch annotation"

    ```java hl_lines="2"
        @Override
        @SqsBatch(value = SampleMessageHandler.class, suppressException = true)
        public String handleRequest(SQSEvent input, Context context) {
            return "{\"statusCode\": 200}";
        }
    ```

=== "Within SqsUtils Utility API"

    ```java hl_lines="3"
        @Override
        public List<String> handleRequest(SQSEvent input, Context context) {
            List<String> returnValues = SqsUtils.batchProcessor(input, true, SampleMessageHandler.class);
    
            return returnValues;
        }
    ```
