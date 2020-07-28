# Lambda Powertools

![aws provider](https://img.shields.io/badge/provider-AWS-orange?logo=amazon-aws&color=ff9900)


A suite of utilities for AWS Lambda Functions that makes tracing with AWS X-Ray, structured logging and creating custom metrics asynchronously easier.

**[📜Documentation](https://awslabs.github.io/aws-lambda-powertools-java/)** | **[Feature request](https://github.com/awslabs/aws-lambda-powertools-java/issues/new?assignees=&labels=feature-request%2C+triage&template=feature_request.md&title=)** | **[🐛Bug Report](https://github.com/awslabs/aws-lambda-powertools-java/issues/new?assignees=&labels=bug%2C+triage&template=bug_report.md&title=)** | **[Detailed blog post](https://aws.amazon.com/blogs/opensource/simplifying-serverless-best-practices-with-lambda-powertools/)**

### Installation

Powertools is available in Maven Central. You can use your favourite dependency management tool to install it

* [maven](https://maven.apache.org/):
```xml
<dependencies>
    ...
    <dependency>
        <groupId>software.aws.lambda</groupId>
        <artifactId>aws-lambda-powertools-java</artifactId>
        <version>YOUR_REQUIRED_VERSION</version>
    </dependency>
    ...
</dependencies>
```

And configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project:

```xml
        <plugin>
             <groupId>com.nickwongdev</groupId>
             <artifactId>aspectj-maven-plugin</artifactId>
             <version>1.12.1</version>
             <configuration>
                 <source>1.8</source>
                 <target>1.8</target>
                 <complianceLevel>1.8</complianceLevel>
                 <aspectLibraries>
                     <aspectLibrary>
                         <groupId>software.aws.lambda</groupId>
                         <artifactId>aws-lambda-powertools-java</artifactId>
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

```
* [gradle](https://gradle.org/):
```
repositories {
    mavenCentral()
}

dependencies {
    powertools 'software.aws.lambda:aws-lambda-powertools-java:YOUR_REQUIRED_VERSION'
}
```

## Credits

* Structured logging initial implementation from [aws-lambda-logging](https://gitlab.com/hadrien/aws_lambda_logging)
* Powertools idea [DAZN Powertools](https://github.com/getndazn/dazn-lambda-powertools/)
* [Gatsby Apollo Theme for Docs](https://github.com/apollographql/gatsby-theme-apollo/tree/master/packages/gatsby-theme-apollo-docs)

## License

This library is licensed under the MIT-0 License. See the LICENSE file.
