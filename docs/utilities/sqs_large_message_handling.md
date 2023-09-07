---
title: SQS Large Message Handling (Deprecated)
description: Utility
---

!!! warning
    This module is now deprecated and will be removed in version 2.
    See [Large Message Handling](large_messages.md) and 
    [the migration guide](large_messages.md#migration-from-the-sqs-large-message-utility)
    for the new module (`powertools-large-messages`) documentation

The large message handling utility handles SQS messages which have had their payloads
offloaded to S3 due to them being larger than the SQS maximum.

The utility automatically retrieves messages which have been offloaded to S3 using the
[amazon-sqs-java-extended-client-lib](https://github.com/awslabs/amazon-sqs-java-extended-client-lib)
client library. Once the message payloads have been processed successful the
utility can delete the message payloads from S3.

This utility is compatible with versions *[1.1.0+](https://github.com/awslabs/amazon-sqs-java-extended-client-lib)* of
amazon-sqs-java-extended-client-lib.

=== "Maven"

```xml

<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>amazon-sqs-java-extended-client-lib</artifactId>
    <version>1.1.0</version>
</dependency>
```
=== "Gradle"

    ```groovy
     dependencies {
        implementation 'com.amazonaws:amazon-sqs-java-extended-client-lib:1.1.0'
    }
    ```

## Install
Depending on your version of Java (either Java 1.8 or 11+), the configuration slightly changes.

=== "Maven Java 11+"

```xml hl_lines="3-7 16 18 24-27"
<dependencies>
...
<dependency>
<groupId>software.amazon.lambda</groupId>
<artifactId>powertools-sqs</artifactId>
<version>{{ powertools.version }}</version>
</dependency>
...
</dependencies>
...
<!-- configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project -->
<build>
<plugins>
...
<plugin>
<groupId>dev.aspectj</groupId>
<artifactId>aspectj-maven-plugin</artifactId>
<version>1.13.1</version>
<configuration>
<source>11</source> <!-- or higher -->
<target>11</target> <!-- or higher -->
<complianceLevel>11</complianceLevel> <!-- or higher -->
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

=== "Maven Java 1.8"

    ```xml hl_lines="3-7 16 18 24-27"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-sqs</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        ...
    </dependencies>
    ...
    <!-- configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project -->
    <build>
        <plugins>
            ...
            <plugin>
                 <groupId>org.codehaus.mojo</groupId>
                 <artifactId>aspectj-maven-plugin</artifactId>
                 <version>1.14.0</version>
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

=== "Gradle Java 11+"

    ```groovy hl_lines="3 11"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-sqs:{{ powertools.version }}'
        }
        
        sourceCompatibility = 11 // or higher
        targetCompatibility = 11 // or higher
    ```

=== "Gradle Java 1.8"

    ```groovy hl_lines="3 11"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '6.6.3'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-sqs:{{ powertools.version }}'
        }
        
        sourceCompatibility = 1.8
        targetCompatibility = 1.8
    ```

## Lambda handler

The annotation `@SqsLargeMessage` should be used with the handleRequest method of a class
which implements `com.amazonaws.services.lambda.runtime.RequestHandler` with
`com.amazonaws.services.lambda.runtime.events.SQSEvent` as the first parameter.

=== "SqsMessageHandler.java"

    ```java hl_lines="6"
    import software.amazon.lambda.powertools.sqs.SqsLargeMessage;

    public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {
    
        @Override
        @SqsLargeMessage
        public String handleRequest(SQSEvent sqsEvent, Context context) {
        // process messages
    
        return "ok";
        }
    }
    ```

`@SqsLargeMessage` creates a default S3 Client `AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient()`.

!!! tip
When the Lambda function is invoked with an event from SQS, each received record
in the SQSEvent is checked to see to validate if it is offloaded to S3.
If it does then `getObject(bucket, key)` will be called, and the payload retrieved.
If there is an error during this process then the function will fail with a `FailedProcessingLargePayloadException`
exception.

    If the request handler method returns without error then each payload will be
    deleted from S3 using `deleteObject(bucket, key)`

To disable deletion of payloads setting the following annotation parameter:

=== "Disable payload deletion"

    ```java hl_lines="3"
    import software.amazon.lambda.powertools.sqs.SqsLargeMessage;

    @SqsLargeMessage(deletePayloads=false)
    public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {
    
    }
    ```

## Utility

If you want to avoid using annotation and have control over error that can happen during payload enrichment use `SqsUtils.enrichedMessageFromS3()`.
It provides you access with a list of `SQSMessage` object enriched from S3 payload.

Original `SQSEvent` object is never mutated. You can also control if the S3 payload should be deleted after successful
processing.

=== "Functional API without annotation"

    ```java hl_lines="9 10 11 14 15 16 17 18 19 20 21 22 27 28 29"
    import software.amazon.lambda.powertools.sqs.SqsLargeMessage;
    import software.amazon.lambda.powertools.sqs.SqsUtils;

    public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {
    
        @Override
        public String handleRequest(SQSEvent sqsEvent, Context context) {
    
             Map<String, String> sqsMessage = SqsUtils.enrichedMessageFromS3(sqsEvent, sqsMessages -> {
                // Some business logic
                Map<String, String> someBusinessLogic = new HashMap<>();
                someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
                return someBusinessLogic;
            });
    
             // Do not delete payload after processing.
             Map<String, String> sqsMessage = SqsUtils.enrichedMessageFromS3(sqsEvent, false, sqsMessages -> {
                // Some business logic
                Map<String, String> someBusinessLogic = new HashMap<>();
                someBusinessLogic.put("Message", sqsMessages.get(0).getBody());
                return someBusinessLogic;
            });
    
             // Better control over exception during enrichment
             try {
                   // Do not delete payload after processing.
                SqsUtils.enrichedMessageFromS3(sqsEvent, false, sqsMessages -> {
                    // Some business logic
                });
             } catch (FailedProcessingLargePayloadException e) {
                 // handle any exception.
             }
    
            return "ok";
        }
    }
    ```

## Overriding the default S3Client

If you require customisations to the default S3Client, you can create your own `S3Client` and pass it to be used by utility either for
**[SqsLargeMessage annotation](#lambda-handler)**, or **[SqsUtils Utility API](#utility)**.

=== "App.java"

    ```java hl_lines="4 5 11"
    import software.amazon.lambda.powertools.sqs.SqsLargeMessage;

    static {
        SqsUtils.overrideS3Client(S3Client.builder()
                .build());
    }

    public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {
    
        @Override
        @SqsLargeMessage
        public String handleRequest(SQSEvent sqsEvent, Context context) {
        // process messages
    
        return "ok";
        }
    }
    ```