---
title: Homepage
description: Powertools for AWS Lambda (Java)
---

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) ![Build status](https://github.com/aws-powertools/powertools-lambda-java/actions/workflows/publish-v2-snapshot.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Faws.oss.sonatype.org%2Fcontent%2Frepositories%2Fsnapshots%2Fsoftware%2Famazon%2Flambda%2Fpowertools-parent%2Fmaven-metadata.xml
)

???+ warning
    You are browsing the documentation for Powertools for AWS Lambda (Java) - v2. This is a snapshot release and not stable! 
    Check out our stable [v1](https://docs.powertools.aws.dev/lambda/java/) documentation if this is not what you wanted.   
    The v2 maven snapshot repository can be found [here](https://aws.oss.sonatype.org/content/repositories/snapshots/software/amazon/lambda/) .

Powertools for AWS Lambda (Java) is a suite of utilities for AWS Lambda Functions that makes tracing with AWS X-Ray, structured logging and creating custom metrics asynchronously easier.

???+ tip
    Powertools for AWS Lambda  is also available for [Python](https://docs.powertools.aws.dev/lambda/python/latest/){target="_blank"}, [TypeScript](https://docs.powertools.aws.dev/lambda/typescript/latest/){target="_blank"}, and [.NET](https://docs.powertools.aws.dev/lambda/dotnet/){target="_blank"}


???+ tip "Looking for a quick run through of the core utilities?"
    Check out [this detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-aws-lambda-powertools-java/) with a practical example. To dive deeper, 
    the [Powertools for AWS Lambda (Java) workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/a7011c82-e4af-4a52-80fa-fcd61f1dacd9/en-US/introduction) is a great next step.

## Tenets

This project separates core utilities that will be available in other runtimes vs general utilities that might not be available across all runtimes.

* **AWS Lambda only** – We optimise for AWS Lambda function environments and supported runtimes only. Utilities might work with web frameworks and non-Lambda environments, though they are not officially supported.
* **Eases the adoption of best practices** – The main priority of the utilities is to facilitate best practices adoption, as defined in the AWS Well-Architected Serverless Lens; all other functionality is optional.
* **Keep it lean** – Additional dependencies are carefully considered for security and ease of maintenance, and prevent negatively impacting startup time.
* **We strive for backwards compatibility** – New features and changes should keep backwards compatibility. If a breaking change cannot be avoided, the deprecation and migration process should be clearly defined.
* **We work backwards from the community** – We aim to strike a balance of what would work best for 80% of customers. Emerging practices are considered and discussed via Requests for Comment (RFCs)
* **Progressive** -  Utilities are designed to be incrementally adoptable for customers at any stage of their Serverless journey. They follow language idioms and their community’s common practices.

## Install

**Quick hello world example using SAM CLI**

You can use [SAM](https://aws.amazon.com/serverless/sam/) to quickly setup a serverless project including Powertools for AWS Lambda (Java).

```bash
sam init
  
Which template source would you like to use?
	1 - AWS Quick Start Templates
	2 - Custom Template Location
Choice: 1

Choose an AWS Quick Start application template
	1 - Hello World Example
	2 - Data processing
	3 - Hello World Example with Powertools for AWS Lambda
	4 - Multi-step workflow
	5 - Scheduled task
	6 - Standalone function
	7 - Serverless API
	8 - Infrastructure event management
	9 - Lambda Response Streaming
	10 - Serverless Connector Hello World Example
	11 - Multi-step workflow with Connectors
	12 - Full Stack
	13 - Lambda EFS example
	14 - DynamoDB Example
	15 - Machine Learning
Template: 3

Which runtime would you like to use?
	1 - dotnet6
	2 - java17
	3 - java11
	4 - java8.al2
	5 - java8
	6 - nodejs18.x
	7 - nodejs16.x
	8 - nodejs14.x
	9 - python3.9
	10 - python3.8
	11 - python3.7
	12 - python3.10
Runtime: 2, 3, 4 or 5
```

**Manual installation**
Powertools for AWS Lambda (Java) dependencies are available in Maven Central. You can use your favourite dependency management tool to install it

* [Maven](https://maven.apache.org/)
* [Gradle](https://gradle.org)

=== "Maven"

    ```xml
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-tracing</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-logging</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-metrics</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        <!-- Make sure to include AspectJ runtime compatible with plugin version -->
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <version>1.9.22</version>
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
                 <version>1.14</version>
                 <configuration>
                     <source>11</source> <!-- or higher -->
                     <target>11</target> <!-- or higher -->
                     <complianceLevel>11</complianceLevel> <!-- or higher -->
                     <aspectLibraries>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-tracing</artifactId>
                         </aspectLibrary>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-logging</artifactId>
                         </aspectLibrary>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-metrics</artifactId>
                         </aspectLibrary>
                     </aspectLibraries>
                 </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.aspectj</groupId>
                        <artifactId>aspectjtools</artifactId>
                        <!-- AspectJ compiler version, in sync with runtime -->
                        <version>1.9.22</version>
                    </dependency>
                </dependencies>
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

        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.2.2'
        }

        // the freefair aspect plugins targets gradle 8.2.1
        // https://docs.freefair.io/gradle-plugins/8.2.2/reference/
        wrapper {
            gradleVersion = "8.2.1"
        }        
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-logging:{{ powertools.version }}'
            aspect 'software.amazon.lambda:powertools-tracing:{{ powertools.version }}'
            aspect 'software.amazon.lambda:powertools-metrics:{{ powertools.version }}'
        }
        
        sourceCompatibility = 11
        targetCompatibility = 11
    ```

???+ tip "Why a different configuration?"
     Powertools for AWS Lambda (Java) is using [AspectJ](https://eclipse.dev/aspectj/doc/released/progguide/starting.html) internally 
    to handle annotations. Recently, in order to support Java 17 we had to move to `dev.aspectj:aspectj-maven-plugin` because  
    `org.codehaus.mojo:aspectj-maven-plugin` does not support Java 17. 
    Under the hood, `org.codehaus.mojo:aspectj-maven-plugin` is based on AspectJ 1.9.7, 
    while `dev.aspectj:aspectj-maven-plugin` is based on AspectJ 1.9.8, compiled for Java 11+.

### Java Compatibility
Powertools for AWS Lambda (Java) supports all Java version from 11 up to 21 as well as the
[corresponding Lambda runtimes](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html).

For the following modules, Powertools for AWS Lambda (Java) leverages the **aspectj** library to provide annotations:
- Logging
- Metrics
- Tracing
- Parameters
- Idempotency
- Validation
- Large messages


You may need to add the good version of `aspectjrt` to your dependencies based on the jdk used for building your function:

```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjrt</artifactId>
    <version>1.9.??</version>
</dependency>
```

Use the following [dependency matrix](https://github.com/eclipse-aspectj/aspectj/blob/master/docs/dist/doc/JavaVersionCompatibility.md) between this library and the JDK:

| JDK version | aspectj version        |
|-------------|------------------------|
| `11-17`     | `1.9.20.1` (or higher) |
| `21`        | `1.9.21` (or higher)   |

## Environment variables

!!! info
    **Explicit parameters take precedence over environment variables.**

| Environment variable                   | Description                                                                            | Utility                   |
|----------------------------------------|----------------------------------------------------------------------------------------|---------------------------|
| **POWERTOOLS_SERVICE_NAME**            | Sets service name used for tracing namespace, metrics dimension and structured logging | All                       |
| **POWERTOOLS_METRICS_NAMESPACE**       | Sets namespace used for metrics                                                        | [Metrics](./core/metrics) |
| **POWERTOOLS_LOGGER_SAMPLE_RATE**      | Debug log sampling                                                                     | [Logging](./core/logging) |
| **POWERTOOLS_LOG_LEVEL**               | Sets logging level                                                                     | [Logging](./core/logging) |
| **POWERTOOLS_LOGGER_LOG_EVENT**        | Enables/Disables whether to log the incoming event when using the aspect               | [Logging](./core/logging) |
| **POWERTOOLS_TRACER_CAPTURE_RESPONSE** | Enables/Disables tracing mode to capture method response                               | [Tracing](./core/tracing) |
| **POWERTOOLS_TRACER_CAPTURE_ERROR**    | Enables/Disables tracing mode to capture method error                                  | [Tracing](./core/tracing) |

