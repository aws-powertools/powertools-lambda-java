# Powertools for AWS Lambda (Java)

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) ![Build status](https://github.com/awslabs/aws-lambda-powertools-java/actions/workflows/build.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/software.amazon.lambda/powertools-parent) [![codecov.io](https://codecov.io/github/awslabs/aws-lambda-powertools-java/branch/master/graphs/badge.svg)](https://app.codecov.io/gh/awslabs/aws-lambda-powertools-java)


Powertools for AWS Lambda (Java) is a developer toolkit to implement Serverless best practices and increase developer velocity.

> Also available in [Python](https://github.com/awslabs/aws-lambda-powertools-python), [TypeScript](https://github.com/awslabs/aws-lambda-powertools-typescript), and [.NET](https://github.com/awslabs/aws-lambda-powertools-dotnet).

**[📜Documentation](https://awslabs.github.io/aws-lambda-powertools-java/)** | **[Feature request](https://github.com/awslabs/aws-lambda-powertools-java/issues/new?assignees=&labels=feature-request%2C+triage&template=feature_request.md&title=)** | **[🐛Bug Report](https://github.com/awslabs/aws-lambda-powertools-java/issues/new?assignees=&labels=bug%2C+triage&template=bug_report.md&title=)** | **[Detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-aws-lambda-powertools-java/)**

### Installation

Powertools for AWS Lambda (Java) is available in Maven Central. You can use your favourite dependency management tool to install it

* [maven](https://maven.apache.org/):
```xml
<dependencies>
    ...
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-tracing</artifactId>
        <version>1.15.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging</artifactId>
        <version>1.15.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-metrics</artifactId>
        <version>1.15.0</version>
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
             <groupId>dev.aspectj</groupId>
             <artifactId>aspectj-maven-plugin</artifactId>
             <version>1.13.1</version>
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

See the **[examples](examples)**  directory for example projects showcasing usage of different utilities.

Have a demo project to contribute which showcase usage of different utilities from powertools? We are happy to accept it [here](CONTRIBUTING.md#security-issue-notifications).

## Credits

* [MkDocs](https://www.mkdocs.org/)
* [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)

## License

This library is licensed under the Apache License, Version 2.0. See the LICENSE file.
