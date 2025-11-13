# Powertools for AWS Lambda (Java)

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=aws-powertools_powertools-lambda-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=aws-powertools_powertools-lambda-java)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=aws-powertools_powertools-lambda-java&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=aws-powertools_powertools-lambda-java)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/aws-powertools/powertools-lambda-java/badge)](https://api.securityscorecards.dev/projects/github.com/aws-powertools/powertools-lambda-java) ![Maven Central](https://img.shields.io/maven-central/v/software.amazon.lambda/powertools-parent) [![codecov.io](https://codecov.io/github/aws-powertools/powertools-lambda-java/branch/main/graphs/badge.svg)](https://app.codecov.io/gh/aws-powertools/powertools-lambda-java)


Powertools for AWS Lambda (Java) is a developer toolkit to implement Serverless best practices and increase developer velocity.

> Also available in [Python](https://github.com/aws-powertools/powertools-lambda-python), [TypeScript](https://github.com/aws-powertools/powertools-lambda-typescript), and [.NET](https://github.com/aws-powertools/powertools-lambda-dotnet).

**[📜Documentation](https://docs.powertools.aws.dev/lambda-java/latest)** | **[Feature request](https://github.com/aws-powertools/powertools-lambda-java/issues/new?template=feature_request.yml)** | **[🐛Bug Report](https://github.com/aws-powertools/powertools-lambda-java/issues/new?template=bug_report.yml)** | **[Detailed blog post](https://aws.amazon.com/blogs/compute/introducing-v2-of-powertools-for-aws-lambda-java/)**

## Installation

Powertools for AWS Lambda (Java) is available in Maven Central. You can use your favourite dependency management tool to install it

### Maven:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-tracing</artifactId>
        <version>2.6.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging-log4j</artifactId>
        <version>2.6.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-metrics</artifactId>
        <version>2.6.0</version>
    </dependency>
    ...
</dependencies>
```

Next, configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project.

<details>
    <summary><b>Maven</b></summary>
    
```xml
<build>
    <plugins>
        ...
        <plugin>
             <groupId>dev.aspectj</groupId>
             <artifactId>aspectj-maven-plugin</artifactId>
             <version>1.14</version>
             <configuration>
                 <source>11</source>
                 <target>11</target>
                 <complianceLevel>11</complianceLevel>
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
</details>

<details>
<summary><b>Gradle</b></summary>

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
            implementation 'software.amazon.lambda:powertools-logging-log4j:{{ powertools.version }}'
            implementation "org.aspectj:aspectjrt:1.9.22"
        }
        
        sourceCompatibility = 11
        targetCompatibility = 11
```
</details>


### Java Compatibility
Powertools for AWS Lambda (Java) supports all Java versions from 11 to 25 in line with the [corresponding Lambda runtimes](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html).

For the modules that provide annotations, Powertools for AWS Lambda (Java) leverages the **aspectj** library.
You may need to add the appropriate version of `aspectjrt` to your dependencies based on the JDK used for building your function:

```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjrt</artifactId>
    <version>1.9.??</version>
</dependency>
```

<details>
    <summary><b>JDK - aspectj dependency matrix</b></summary>

Use the following [dependency matrix](https://github.com/eclipse-aspectj/aspectj/blob/master/docs/release/JavaVersionCompatibility.adoc) to understand which AspectJ version to use based on your JDK version:

| JDK version | aspectj version        |
|-------------|------------------------|
| `11-17`     | `1.9.20.1` (or higher) |
| `21`        | `1.9.21` (or higher)   |
| `25`        | `1.9.25` (or higher)   |

</details>

## Examples

See the latest release of the **[examples](https://github.com/aws-powertools/powertools-lambda-java/tree/main/examples)** for example projects showcasing usage of different utilities.

Have a demo project to contribute which showcase usage of different utilities from powertools? We are happy to accept it [here](CONTRIBUTING.md#security-issue-notifications).

## How to support Powertools for AWS Lambda (Java)?

### Becoming a reference customer

Knowing which companies are using this library is important to help prioritize the project internally. If your company is using Powertools for AWS Lambda (Java), you can request to have your name and logo added to the README file by raising a [Support Powertools for AWS Lambda (Java) (become a reference)](https://github.com/aws-powertools/powertools-lambda-java/issues/new?template=support_powertools.yml) issue.

The following companies, among others, use Powertools:

* [Capital One](https://www.capitalone.com/)
* [CPQi (Exadel Financial Services)](https://cpqi.com/)
* [Europace AG](https://europace.de/)
* [Vertex Pharmaceuticals](https://www.vrtx.com/)

## Connect

- **Powertools for AWS Lambda on Discord**: `#java` - **[Invite link](https://discord.gg/B8zZKbbyET)**
- **Email**: <aws-powertools-maintainers@amazon.com>

## Security disclosures

If you think you’ve found a potential security issue, please do not post it in the Issues.  Instead, please follow the instructions [here](https://aws.amazon.com/security/vulnerability-reporting/) or [email AWS security directly](mailto:aws-security@amazon.com).

## License

This library is licensed under the MIT-0 License. See the [LICENSE](https://github.com/aws-powertools/powertools-lambda-java/blob/main/LICENSE) file.
