# Powertools for AWS Lambda (Java)

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) ![Build status](https://github.com/aws-powertools/powertools-lambda-java/actions/workflows/build.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/software.amazon.lambda/powertools-parent) [![codecov.io](https://codecov.io/github/aws-powertools/powertools-lambda-java/branch/main/graphs/badge.svg)](https://app.codecov.io/gh/aws-powertools/powertools-lambda-java)


Powertools for AWS Lambda (Java) is a developer toolkit to implement Serverless best practices and increase developer velocity.

> Also available in [Python](https://github.com/aws-powertools/powertools-lambda-python), [TypeScript](https://github.com/aws-powertools/powertools-lambda-typescript), and [.NET](https://github.com/aws-powertools/powertools-lambda-dotnet).

**[📜Documentation](https://docs.powertools.aws.dev/lambda-java/)** | **[Feature request](https://github.com/aws-powertools/powertools-lambda-java/issues/new?assignees=&labels=feature-request%2C+triage&template=feature_request.md&title=)** | **[🐛Bug Report](https://github.com/aws-powertools/powertools-lambda-java/issues/new?assignees=&labels=bug%2C+triage&template=bug_report.md&title=)** | **[Detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-aws-lambda-powertools-java/)**

### Installation

Powertools for AWS Lambda (Java) is available in Maven Central. You can use your favourite dependency management tool to install it

#### Maven:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-tracing</artifactId>
        <version>1.16.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging</artifactId>
        <version>1.16.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-metrics</artifactId>
        <version>1.16.0</version>
    </dependency>
    ...
</dependencies>
```

And configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project:

For Java 11+, use the following:
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

For Java 8, use the following:
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
#### gradle

For Java 11+:

    ```groovy
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0'
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

For Java8:

    ```groovy
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '6.6.3'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-logging:{{ powertools.version }}'
            aspect 'software.amazon.lambda:powertools-tracing:{{ powertools.version }}'
            aspect 'software.amazon.lambda:powertools-metrics:{{ powertools.version }}'
        }
        
        sourceCompatibility = 1.8
        targetCompatibility = 1.8
    ```


## Example

See the **[examples](examples)**  directory for example projects showcasing usage of different utilities.

Have a demo project to contribute which showcase usage of different utilities from powertools? We are happy to accept it [here](CONTRIBUTING.md#security-issue-notifications).

## Credits

* [MkDocs](https://www.mkdocs.org/)
* [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)

## License

This library is licensed under the Apache License, Version 2.0. See the LICENSE file.
