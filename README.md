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
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-tracing</artifactId>
        <version>0.1.0-beta</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging</artifactId>
        <version>0.1.0-beta</version>
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
             <version>1.11</version>
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
**Note:** If you are working with Lambda on runtime post java8, please refer [issue](https://github.com/awslabs/aws-lambda-powertools-java/issues/50) for workaround

### Logging Configuration
Powertools extends the functionality of Log4J. Below is an example `log4j2.xml` file, with the `LambdaJsonLayout` configured.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="com.amazonaws.services.lambda.runtime.log4j2">
    <Appenders>
        <Console name="JsonAppender" target="SYSTEM_OUT">
            <LambdaJsonLayout compact="true" eventEol="true"/>
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="JsonLogger" level="INFO" additivity="false">
            <AppenderRef ref="JsonAppender"/>
        </Logger>
        <Root level="info">
            <AppenderRef ref="JsonAppender"/>
        </Root>
    </Loggers>
</Configuration>
```

## Credits

* [Gatsby Apollo Theme for Docs](https://github.com/apollographql/gatsby-theme-apollo/tree/master/packages/gatsby-theme-apollo-docs)

## License

This library is licensed under the Apache License, Version 2.0. See the LICENSE file.



Cold Start

946.58 ms - original
943.96 ms - power tools