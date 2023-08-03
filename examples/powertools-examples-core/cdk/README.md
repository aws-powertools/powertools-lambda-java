#  Powertools for AWS Lambda (Java) - Core Utilities Example with CDK

This project demonstrates the Lambda for Powertools Java module deployed using [Cloud Development Kit](https://aws.amazon.com/cdk/).

For general information on project structure similar to all architectures, you can refer to the parent [README](../README.md)

## Configuration
CDK uses the following project structure:
- [app](./app) - stores the source code of your application, which is similar between all examples
- [infra](./infra) - stores the definition of your infrastructure
  - [cdk.json](./infra/cdk.json) - tells the CDK Toolkit how to execute your app
  - [CdkApp](./infra/src/main/java/cdk/CdkApp.java) - bootstraps your stack, taking AWS `account` and `region` as input
  - [CdkStack](./infra/src/main/java/cdk/CdkStack.java) - defines the Lambda function to be deployed as well as API Gateway for it.

It is a [Maven](https://maven.apache.org/)-based project, so you can open this project with any Maven compatible Java IDE to build and run tests.


## Deploy the sample application
To deploy the example, check out the instructions for getting
started with SAM in [the examples directory](../../README.md)

The minimum to deploy the app should be
```bash 
cdk bootstrap && cdk deploy
```

## Useful commands

* `mvn package`     compile and run tests
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation