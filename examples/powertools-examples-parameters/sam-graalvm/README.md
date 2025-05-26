# Powertools for AWS Lambda (Java) - Parameters Example with SAM on GraalVM

This project contains an example of Lambda function using the parameters module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/parameters/).

The example uses the [SSM Parameter Store](https://docs.powertools.aws.dev/lambda/java/utilities/parameters/#ssm-parameter-store)
and the [Secrets Manager](https://docs.powertools.aws.dev/lambda/java/utilities/parameters/#secrets-manager) to inject
runtime parameters into the application. 
Have a look at [ParametersFunction.java](src/main/java/org/demo/parameters/ParametersFunction.java) for the full details.

## Configuration

- SAM uses [template.yaml](template.yaml) to define the application's AWS resources.
  This file defines the Lambda function to be deployed as well as API Gateway for it.

- Set the environment to use GraalVM

```shell
export JAVA_HOME=<path to GraalVM>
```

## Build the sample application

- Build the Docker image that will be used as the environment for SAM build:

```shell
docker build --platform linux/amd64 . -t powertools-examples-parameters-sam-graalvm
```

- Build the SAM project using the docker image

```shell
sam build --use-container --build-image powertools-examples-parameters-sam-graalvm
```

#### [Optional] Building with -SNAPSHOT versions of PowerTools

- If you are testing the example with a -SNAPSHOT version of PowerTools, the maven build inside the docker image will fail. This is because the -SNAPSHOT version of the PowerTools library that you are working on is still not available in maven central/snapshot repository.
  To get around this, follow these steps:
    - Create the native image using the `docker` command below on your development machine. The native image is created in the `target` directory.
        - `` docker run --platform linux/amd64  -it -v `pwd`:`pwd` -w `pwd` -v ~/.m2:/root/.m2 powertools-examples-parameters-sam-graalvm mvn clean -Pnative-image package -DskipTests ``
    - Edit the [`Makefile`](Makefile) remove this line
        - `mvn clean package -P native-image`
    - Build the SAM project using the docker image
        - `sam build --use-container --build-image powertools-examples-parameters-sam-graalvm`

## Deploy the sample application

- SAM deploy

```shell
sam deploy
```

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../../README.md)

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
