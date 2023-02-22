# Deserialization

This project contains an example of Lambda function using the serialization utilities module of Lambda Powertools for Java. For more information on this module, please refer to the [documentation](https://awslabs.github.io/aws-lambda-powertools-java/utilities/serialization/).

## Deploy the sample application

This sample is based on Serverless Application Model (SAM) and you can use the SAM Command Line Interface (SAM CLI) to build it and deploy it to AWS.

To use the SAM CLI, you need the following tools.

* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Java11 - [Install the Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

To build and deploy your application for the first time, run the following in your shell:

```bash
sam build
sam deploy --guided
```
