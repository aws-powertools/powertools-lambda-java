# Lambda Powertools for Java - Parameters Example

This project contains an example of Lambda function using the parameters module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/parameters/).

The example uses the [SSM Parameter Store](https://docs.powertools.aws.dev/lambda/java/utilities/parameters/#ssm-parameter-store)
and the [Secrets Manager](https://docs.powertools.aws.dev/lambda/java/utilities/parameters/#secrets-manager) to inject
runtime parameters into the application. 
Have a look at [ParametersFunction.java](src/main/java/org/demo/parameters/ParametersFunction.java) for the full details.

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md)

## Test the application

First, hit the URL of the application. You can do this with curl or your browser:

```bash
 curl https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/params/
```
You will get your IP address back. The contents of the logs will be more interesting, and show you the values
of the parameters injected into the handler:

```bash
sam logs --stack-name $MY_STACK_NAME --tail
```

```json
{
  ...
  "thread": "main",
  "level": "INFO",
  "loggerName": "org.demo.parameters.ParametersFunction",
  "message": "secretjsonobj=MyObject{id=23443, code='hk38543oj24kn796kp67bkb234gkj679l68'}\n",
  ...
}
```
