---
title: Homepage
description: AWS Lambda Powertools for Java
---

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) ![Build status](https://github.com/awslabs/aws-lambda-powertools-java/actions/workflows/build.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/software.amazon.lambda/powertools-parent)

Powertools is a suite of utilities for AWS Lambda Functions that makes tracing with AWS X-Ray, structured logging and creating custom metrics asynchronously easier.

???+ tip
    Powertools is also available for [Python](https://awslabs.github.io/aws-lambda-powertools-python/){target="_blank"}, [TypeScript](https://awslabs.github.io/aws-lambda-powertools-typescript/){target="_blank"}, and [.NET](https://awslabs.github.io/aws-lambda-powertools-dotnet/){target="_blank"}


!!! tip "Looking for a quick run through of the core utilities?"
    Check out [this detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-aws-lambda-powertools-java/) with a practical example.

## Tenets

This project separates core utilities that will be available in other runtimes vs general utilities that might not be available across all runtimes.

* **AWS Lambda only** – We optimise for AWS Lambda function environments and supported runtimes only. Utilities might work with web frameworks and non-Lambda environments, though they are not officially supported.
* **Eases the adoption of best practices** – The main priority of the utilities is to facilitate best practices adoption, as defined in the AWS Well-Architected Serverless Lens; all other functionality is optional.
* **Keep it lean** – Additional dependencies are carefully considered for security and ease of maintenance, and prevent negatively impacting startup time.
* **We strive for backwards compatibility** – New features and changes should keep backwards compatibility. If a breaking change cannot be avoided, the deprecation and migration process should be clearly defined.
* **We work backwards from the community** – We aim to strike a balance of what would work best for 80% of customers. Emerging practices are considered and discussed via Requests for Comment (RFCs)
* **Progressive** -  Utilities are designed to be incrementally adoptable for customers at any stage of their Serverless journey. They follow language idioms and their community’s common practices.

## Install

Powertools dependencies are available in Maven Central. You can use your favourite dependency management tool to install it

* [Maven](https://maven.apache.org/)
* [Gradle](https://gradle.org)

**Quick hello world examples using SAM CLI**

You can use [SAM](https://aws.amazon.com/serverless/sam/) to quickly setup a serverless project including AWS Lambda Powertools for Java.

```bash
  sam init --location gh:aws-samples/cookiecutter-aws-sam-powertools-java
```

For more information about the project and available options refer to this [repository](https://github.com/aws-samples/cookiecutter-aws-sam-powertools-java/blob/main/README.md)

=== "Maven"

    ```xml hl_lines="3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55" 
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
    plugins{
        id 'java'
        id 'io.freefair.aspectj.post-compile-weaving' version '6.3.0'
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

## Environment variables

!!! info
    **Explicit parameters take precedence over environment variables.**

| Environment variable | Description | Utility |
| ------------------------------------------------- | --------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| **POWERTOOLS_SERVICE_NAME** | Sets service name used for tracing namespace, metrics dimension and structured logging | All |
| **POWERTOOLS_METRICS_NAMESPACE** | Sets namespace used for metrics | [Metrics](./core/metrics) |
| **POWERTOOLS_LOGGER_SAMPLE_RATE** | Debug log sampling | [Logging](./core/logging) |
| **POWERTOOLS_LOG_LEVEL** | Sets logging level | [Logging](./core/logging) |
| **POWERTOOLS_TRACER_CAPTURE_RESPONSE** | Enables/Disables tracing mode to capture method response | [Tracing](./core/tracing) |
| **POWERTOOLS_TRACER_CAPTURE_ERROR** | Enables/Disables tracing mode to capture method error | [Tracing](./core/tracing) |