---
title: Aurora stream Handling
description: Utility
---

The Aurora Stream processing utility provides a way to process Kinesis streams from Aurora

**Key features**

* Decrypt Kinesis events 
* A simple interface to customize the event handling

**Background** 

When using Kinesis Event from Aurora data activity stream you need to decrypt and serialize the message included in the Lambda event. This operation requires transformations and the KMS integration to get the full message from the Aurora database.
With this module you will be able to get the stream content easily just including some libraries.

## Install

To install this utility, add the following dependency to your project.

=== "Maven"
    ```xml hl_lines="3 4 5 6 7 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-aurora</artifactId>
            <version>${powertools.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>kms</artifactId>
            <version>2.17.120</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>apache-client</artifactId>
            <version>2.17.120</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>utils</artifactId>
            <version>2.17.120</version>
        </dependency>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-encryption-sdk-java</artifactId>
            <version>2.3.3</version>
        </dependency>
    ```

=== "Gradle"

    ```groovy
    dependencies {
    ...
    implementation 'software.amazon.lambda:powertools-aurora:1.10.3'
    }
    ```

## Functional Interface DataStreamHandler

The AuroraUtils class requires an implementation of functional interface `DataStreamHandler`.

This implementation is responsible for processing each individual message from the kinesis event, and to raise an exception if unable to process any of the messages sent.

If you require access to the result of processed messages, you can use this utility. The result from calling **`#!java AuroraUtils#process()`** on the context manager will be a list of all the return values
from your **`#!java DataStreamHandler#process()`** function.


    ```java 
    package helloworld;
    
    import java.util.List;
    
    import software.amazon.lambda.powertools.aurora.DataStreamHandler;
    import software.amazon.lambda.powertools.aurora.model.PostgresActivityEvent;
    import software.amazon.lambda.powertools.aurora.AuroraUtils;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
    
    /**
    * Handler for requests to Lambda function.
    */
    
        public class App implements RequestHandler<KinesisEvent, String> {
    
        @Override
        public String handleRequest(final KinesisEvent input, final Context context) {
            List values = AuroraUtils.process(input, SampleDataHandler.class);
            return "ok";
        }
    
        public class SampleDataHandler implements DataStreamHandler<Object> {
           @Override
           public PostgresActivityEvent process(PostgresActivityEvent record) {
               //doSomething(record);
               return record;
           }
        }
    }
    ```