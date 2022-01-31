# AWS Lambda Powertools (Java)

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) ![Build status](https://github.com/awslabs/aws-lambda-powertools-java/actions/workflows/build.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/software.amazon.lambda/powertools-parent)

A suite of utilities for AWS Lambda Functions that makes tracing with AWS X-Ray, structured logging and creating custom metrics asynchronously easier. ([AWS Lambda Powertools Python](https://github.com/awslabs/aws-lambda-powertools-python) is also available).

**[📜Documentation](https://awslabs.github.io/aws-lambda-powertools-java/)** | **[Feature request](https://github.com/awslabs/aws-lambda-powertools-java/issues/new?assignees=&labels=feature-request%2C+triage&template=feature_request.md&title=)** | **[🐛Bug Report](https://github.com/awslabs/aws-lambda-powertools-java/issues/new?assignees=&labels=bug%2C+triage&template=bug_report.md&title=)** | **[Detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-aws-lambda-powertools-java/)**

### Installation

Powertools is available in Maven Central. You can use your favourite dependency management tool to install it

* [maven](https://maven.apache.org/):
```xml
<dependencies>
    ...
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-tracing</artifactId>
        <version>1.10.2</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging</artifactId>
        <version>1.10.2</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-metrics</artifactId>
        <version>1.10.2</version>
    </dependency>
    ...
</dependencies>
```

And configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project:

```xml
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
                         <artifactId>powertools-logging</artifactId>
                     </aspectLibrary>
                     <aspectLibrary>
                         <groupId>software.amazon.lambda</groupId>
                         <artifactId>powertools-tracing</artifactId>
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

## Example

See **[example](https://github.com/aws-samples/aws-lambda-powertools-examples/tree/main/java)** for example project showcasing usage of different utilities. 
Have a demo project to contribute which showcase usage of different utilities from powertools? We are happy to accept it [here](https://github.com/aws-samples/aws-lambda-powertools-examples/blob/main/CONTRIBUTING.md#security-issue-notifications).

## Credits

* [MkDocs](https://www.mkdocs.org/)
* [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)

## License

This library is licensed under the Apache License, Version 2.0. See the LICENSE file.
