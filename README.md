# Powertools for AWS Lambda (Java)

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) ![Build status](https://github.com/aws-powertools/powertools-lambda-java/actions/workflows/build.yml/badge.svg) ![Maven Central](https://img.shields.io/maven-central/v/software.amazon.lambda/powertools-parent) [![codecov.io](https://codecov.io/github/aws-powertools/powertools-lambda-java/branch/main/graphs/badge.svg)](https://app.codecov.io/gh/aws-powertools/powertools-lambda-java)


Powertools for AWS Lambda (Java) is a developer toolkit to implement Serverless best practices and increase developer velocity.

> Also available in [Python](https://github.com/aws-powertools/powertools-lambda-python), [TypeScript](https://github.com/aws-powertools/powertools-lambda-typescript), and [.NET](https://github.com/aws-powertools/powertools-lambda-dotnet).

**[📜Documentation](https://docs.powertools.aws.dev/lambda-java/)** | **[Feature request](https://github.com/aws-powertools/powertools-lambda-java/issues/new?assignees=&labels=feature-request%2C+triage&template=feature_request.md&title=)** | **[🐛Bug Report](https://github.com/aws-powertools/powertools-lambda-java/issues/new?assignees=&labels=bug%2C+triage&template=bug_report.md&title=)** | **[Detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-aws-lambda-powertools-java/)**

## Installation

Powertools for AWS Lambda (Java) is available in Maven Central. You can use your favourite dependency management tool to install it

### Maven:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-tracing</artifactId>
        <version>1.19.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging</artifactId>
        <version>1.19.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-metrics</artifactId>
        <version>1.19.0-SNAPSHOT</version>
    </dependency>
    ...
</dependencies>
```

Next, configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project. A different configuration is needed for projects on Java 8. 

<details>
    <summary><b>Maven - Java 11 and newer</b></summary>
    
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
</details>

<details>
<summary><b>Maven - Java 8</b></summary>    
    
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
</details>

<details>
<summary><b>Gradle - Java 11+</b></summary>

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
            implementation "org.aspectj:aspectjrt:1.9.8.RC3"
        }
        
        sourceCompatibility = 11
        targetCompatibility = 11
```
</details>

<details>
<summary><b>Gradle - Java 8</b></summary>    

```groovy
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '6.6.3'
        }
        
        // the freefair aspect plugins targets gradle 7.6.1
        // https://docs.freefair.io/gradle-plugins/6.6.3/reference/
        wrapper {
            gradleVersion = "7.6.1"
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
</details>

### Java Compatibility
Powertools for AWS Lambda (Java) supports all Java version from 8 up to 21 as well as the
[corresponding Lambda runtimes](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html).
For the modules that provide annotations, Powertools for AWS Lambda (Java) leverages the **aspectj** library.
You may need to add the good version of `aspectjrt` to your dependencies based on the JDK used for building your function:

```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjrt</artifactId>
    <version>1.9.??</version>
</dependency>
```

<details>
    <summary><b>JDK - aspectj dependency matrix</b></summary>

| JDK version | aspectj version |
|-------------|-----------------|
| `1.8`       | `1.9.7`         |
| `11-17`     | `1.9.20.1`      |
| `21`        | `1.9.21`        |

More info [here](https://github.com/aws-powertools/powertools-lambda-java/pull/1519/files#diff-b335630551682c19a781afebcf4d07bf978fb1f8ac04c6bf87428ed5106870f5R191).

</details>

## Examples

See the latest release of the **[examples](https://github.com/aws-powertools/powertools-lambda-java/tree/v1.19.0-SNAPSHOT/examples)** for example projects showcasing usage of different utilities.

Have a demo project to contribute which showcase usage of different utilities from powertools? We are happy to accept it [here](CONTRIBUTING.md#security-issue-notifications).

## How to support Powertools for AWS Lambda (Java)?

### Becoming a reference customer

Knowing which companies are using this library is important to help prioritize the project internally. If your company is using Powertools for AWS Lambda (Java), you can request to have your name and logo added to the README file by raising a [Support Powertools for AWS Lambda (Java) (become a reference)](https://github.com/aws-powertools/powertools-lambda-java/issues/new?assignees=&labels=customer-reference&template=support_powertools.yml&title=%5BSupport+Lambda+Powertools%5D%3A+%3Cyour+organization+name%3E) issue.

The following companies, among others, use Powertools:

* [Capital One](https://www.capitalone.com/)
* [Caylent](https://caylent.com/)
* [CPQi (Exadel Financial Services)](https://cpqi.com/)
* [Europace AG](https://europace.de/)
* [Vertex Pharmaceuticals](https://www.vrtx.com/)

## Credits

* [MkDocs](https://www.mkdocs.org/)
* [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)

## Connect

* **Powertools for AWS Lambda on Discord**: `#java` - **[Invite link](https://discord.gg/B8zZKbbyET)**
* **Email**: <aws-lambda-powertools-feedback@amazon.com>

## Security disclosures

If you think you’ve found a potential security issue, please do not post it in the Issues.  Instead, please follow the instructions [here](https://aws.amazon.com/security/vulnerability-reporting/) or [email AWS security directly](mailto:aws-security@amazon.com).

## License

This library is licensed under the Apache License, Version 2.0. See the LICENSE file.
