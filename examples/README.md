#  Powertools for AWS Lambda (Java) Examples 

This directory holds example projects demoing different components of the Powertools for AWS Lambda (Java).
Each example can be copied from its subdirectory and used independently of the rest of this repository.

## Examples

* [powertools-examples-core](powertools-examples-core) - Demonstrates the core logging, tracing, and metrics modules with different build tools and languages 
  * [CDK](./powertools-examples-core-utilities/cdk)
  * [Gradle](./powertools-examples-core-utilities/gradle)
  * [SAM](./powertools-examples-core-utilities/sam) 
  * [Serverless](./powertools-examples-core-utilities/serverless)
  * [Kotlin](./powertools-examples-core-utilities/kotlin)
* [powertools-examples-idempotency](powertools-examples-idempotency) - An idempotent HTTP API
* [powertools-examples-parameters](powertools-examples-parameters) - Uses the parameters module to provide runtime parameters to a function
* [powertools-examples-serialization](powertools-examples-serialization) - Uses the serialization module to serialize and deserialize API Gateway & SQS payloads
* [powertools-examples-sqs](powertools-examples-sqs) - Processes SQS batch requests (**Deprecated** - will be replaced by `powertools-examples-batch` in version 2 of this library)
* [powertools-examples-validation](powertools-examples-validation) - Uses the validation module to validate user requests received via API Gateway
* [powertools-examples-cloudformation](powertools-examples-cloudformation) - Deploys a Cloudformation custom resource
* [powertools-examples-batch](powertools-examples-batch) - Examples for each of the different batch processing deployments

## Working with AWS Serverless Application Model (SAM) Examples
Many of the examples use [AWS Serverless Application Model](https://aws.amazon.com/serverless/sam/) (SAM). To get started
with them, you can use the SAM Command Line Interface (SAM CLI) to build it and deploy an example to AWS. 

To use the SAM CLI, you need the following tools.

* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Java11 - [Install the Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

To learn more about SAM, 
[check out the developer guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/using-sam-cli.html).
You can use the CLI to [test events locally](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/using-sam-cli-local-invoke.html),
and [run the application locally](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/using-sam-cli-local-start-api.html),
amongst other things.

To build and deploy an example application for the first time, run the following in your shell:

```bash
# Switch to the directory containing an example for the powertools-idempotency module
$ cd powertools-examples-idempotency

# Build and deploy the example
$ sam build
$ sam deploy --guided
```

The first command will build the source of your application. The second command will package and deploy your application to AWS, with a series of prompts:

* **Stack Name**: The name of the stack to deploy to CloudFormation. This should be unique to your account and region, and a good starting point would be something matching your project name.
* **AWS Region**: The AWS region you want to deploy your app to.
* **Confirm changes before deploy**: If set to yes, any change sets will be shown to you before execution for manual review. If set to no, the AWS SAM CLI will automatically deploy application changes.
* **Allow SAM CLI IAM role creation**: Many AWS SAM templates, including this example, create AWS IAM roles required for the AWS Lambda function(s) included to access AWS services. By default, these are scoped down to minimum required permissions. To deploy an AWS CloudFormation stack which creates or modified IAM roles, the `CAPABILITY_IAM` value for `capabilities` must be provided. If permission isn't provided through this prompt, to deploy this example you must explicitly pass `--capabilities CAPABILITY_IAM` to the `sam deploy` command.
* **Save arguments to samconfig.toml**: If set to yes, your choices will be saved to a configuration file inside the project, so that in the future you can just re-run `sam deploy` without parameters to deploy changes to your application.

You can find your API Gateway Endpoint URL in the output values displayed after deployment.

If you're not using SAM, you can look for examples for other tools under [powertools-examples-core](./powertools-examples-core)

### External examples

You can find more examples in the https://github.com/aws/aws-sam-cli-app-templates project:

* [Java 8 + Maven](https://github.com/aws/aws-sam-cli-app-templates/tree/master/java8/hello-pt-maven)
* [Java 8 on Amazon Linux 2 + Maven](https://github.com/aws/aws-sam-cli-app-templates/tree/master/java8.al2/hello-pt-maven)
* [Java 11 + Maven](https://github.com/aws/aws-sam-cli-app-templates/tree/master/java11/hello-pt-maven)
* [Java 17 + Maven](https://github.com/aws/aws-sam-cli-app-templates/tree/master/java17/hello-pt-maven)
* [Java 17 + Gradle](https://github.com/aws/aws-sam-cli-app-templates/tree/master/java17/hello-pt-gradle)


### SAM - Other Tools 

If you prefer to use an integrated development environment (IDE) to build and test your application, you can use the AWS Toolkit.  
The AWS Toolkit is an open source plug-in for popular IDEs that uses the SAM CLI to build and deploy serverless applications on AWS. The AWS Toolkit also adds a simplified step-through debugging experience for Lambda function code. See the following links to get started.

* [PyCharm](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [IntelliJ](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [VS Code](https://docs.aws.amazon.com/toolkit-for-vscode/latest/userguide/welcome.html)
* [Visual Studio](https://docs.aws.amazon.com/toolkit-for-visual-studio/latest/user-guide/welcome.html)