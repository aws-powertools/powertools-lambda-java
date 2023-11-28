#  Powertools for AWS Lambda (Java) - Cloudformation Custom Resource Example

This project contains an example of Lambda function using the CloudFormation module of Powertools for AWS Lambda in Java. For more information on this module, please refer to the [documentation](https://awslabs.github.io/aws-lambda-powertools-java/utilities/custom_resources/).

In this example you pass in a bucket name as a parameter and upon CloudFormation events a call is made to a lambda.  That lambda attempts to create the bucket on CREATE events, create a new bucket if the name changes with an UPDATE event and delete the bucket upon DELETE events.   

## Deploy the sample application

This sample can be used either with the Serverless Application Model (SAM) or with CDK.

### Deploy with SAM CLI
To deploy it using the SAM CLI, check out the instructions for getting started in [the examples directory](../README.md)
Run the following in your shell: 

```bash
cd infra/sam
sam build
sam deploy --guided --parameter-overrides BucketNameParam=my-unique-bucket-20230718
```

### Deploy with CDK
To use CDK you need the following tools.

* CDK - [Install CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html)
* Java 11 - [Install Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

To build and deploy this application for the first time, run the following in your shell:

```bash
cd infra/cdk
mvn package
cdk synth
cdk deploy -c BucketNameParam=my-unique-bucket-20230718
```