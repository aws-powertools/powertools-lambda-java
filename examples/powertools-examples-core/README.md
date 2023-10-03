#  Powertools for AWS Lambda (Java) - Core Utilities Example

This project demonstrates the Lambda for Powertools Java module - including 
[logging](https://docs.powertools.aws.dev/lambda/java/core/logging/),
[tracing](https://docs.powertools.aws.dev/lambda/java/core/tracing/), and
[metrics](https://docs.powertools.aws.dev/lambda/java/core/metrics/).

We provide examples for the following infrastructure-as-code tools:

* [AWS SAM](sam/) 
* [AWS CDK](cdk/)
* [Serverless framework](serverless/)

We also provide an example showing the integration of SAM, Powertools, and Gradle:

* [AWS SAM with a Gradle build](gradle/)

## Java
For each of the tools, the example application is the same, and consists of the following files:

- [App.java](sam/src/main/java/helloworld/App.java) - Code for the application's Lambda function.
- [AppTests.java](sam/src/test/java/helloworld/AppTest.java) - Unit tests for the application code.
- [events](sam/events/event.json) - Invocation events that you can use to invoke the function.

Configuration files and deployment process for each tool are described in corresponding README files.

## Kotlin
For each of the tools, the example application is the same, and consists of the following files:

- [App.kt](kotlin/src/main/kotlin/helloworld/App.kt) - Code for the application's Lambda function.
- [AppTests.kt](kotlin/src/test/kotlin/helloworld/AppTest.kt) - Unit tests for the application code.
- [events](kotlin/events/event.json) - Invocation events that you can use to invoke the function.

Configuration files and deployment process for each tool are described in corresponding README files.

## Test the application

Once the app is deployed, you can invoke the endpoint like this:

```bash
 curl https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/hello/
```

The response itself isn't particularly interesting - you will get back some information about your IP address.  If 
you go to the Lambda Console and locate the lambda you have deployed, then click the "Monitoring" tab you will
be able to find:

* **View X-Ray traces** -  Display the traces captured by the traces module. These include subsegments for the
different function calls within the example
* **View Cloudwatch logs** - Display the structured logging output of the example

Likewise, from the CloudWatch dashboard, under **Metrics**, **all metrics**,  you will find the namespaces `Another`
and `ServerlessAirline`. The values in each of these are published by the code in
[App.java](sam/src/main/java/helloworld/App.java). 
