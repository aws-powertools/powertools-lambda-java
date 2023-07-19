# Lambda Powertools for Java - Core Utilities Example

This project demonstrates the Lambda for Powertools Java module - including 
[logging](https://docs.powertools.aws.dev/lambda/java/core/logging/),
[tracing](https://docs.powertools.aws.dev/lambda/java/core/tracing/), and
[metrics](https://docs.powertools.aws.dev/lambda/java/core/metrics/).

It is made up of the following:

- [App.java](src/main/java/helloworld/App.java) - Code for the application's Lambda function.
- [events](events) - Invocation events that you can use to invoke the function.
- [AppTests.java](src/test/java/helloworld/AppTest.java) - Unit tests for the application code. 
- [template.yaml](template.yaml) - A template that defines the application's AWS resources.

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md)

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
[App.java](src/main/java/helloworld/App.java). 

You can also watch the trace information or log information using the SAM CLI:
```bash
# Tail the logs
sam logs --tail $MY_STACK

# Tail the traces
sam traces --tail
```