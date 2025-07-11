#  Powertools for AWS Lambda (Java) - Serialization Example

This project contains an example of Lambda function using the serialization utilities module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/latest/utilities/serialization/).

The project contains two `RequestHandler`s - 

* [APIGatewayRequestDeserializationFunction](src/main/java/org/demo/serialization/APIGatewayRequestDeserializationFunction.java) - Uses the serialization library to deserialize an API Gateway request body
* [SQSEventDeserializationFunction](src/main/java/org/demo/serialization/SQSEventDeserializationFunction.java) - Uses the serialization library to deserialize an SQS message body

In both cases, the output of the serialized message will be printed to the function logs. The message format
in JSON looks like this:

```json
{
  "id":1234, 
  "name":"product", 
  "price":42
}
```

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../../README.md)

## Configuration

- Set the environment to use GraalVM

```shell
export JAVA_HOME=<path to GraalVM>
```

## Build the sample application

- Build the Docker image that will be used as the environment for SAM build:

```shell
docker build --platform linux/amd64 . -t powertools-examples-serialization-sam-graalvm
```

- Build the SAM project using the docker image

```shell
sam build --use-container --build-image powertools-examples-serialization-sam-graalvm
```

#### [Optional] Building with -SNAPSHOT versions of PowerTools

- If you are testing the example with a -SNAPSHOT version of PowerTools, the maven build inside the docker image will fail. This is because the -SNAPSHOT version of the PowerTools library that you are working on is still not available in maven central/snapshot repository.
  To get around this, follow these steps:
    - Create the native image using the `docker` command below on your development machine. The native image is created in the `target` directory.
        - `` docker run --platform linux/amd64  -it -v `pwd`:`pwd` -w `pwd` -v ~/.m2:/root/.m2 powertools-examples-serialization-sam-graalvm mvn clean -Pnative-image package -DskipTests ``
    - Edit the [`Makefile`](Makefile) remove this line
        - `mvn clean package -P native-image`
    - Build the SAM project using the docker image
        - `sam build --use-container --build-image powertools-examples-serialization-sam-graalvm`


## Test the application

### 1. API Gateway Endpoint

To test the HTTP endpoint, we can post a product to the test URL:

```bash
curl -X POST https://gct1q3gaw0.execute-api.eu-west-1.amazonaws.com/Prod/product/ -H "Content-Type: application/json" -d '{"id": 1234, "name": "product", "price": 42}'
```

The result will indicate that the handler has successfully deserialized the request body:

```
Received request for productId: 1234
```

If we look at the logs using `sam logs --tail --stack-name $MY_STACK`, we will see the full deserialized request:

```json
{
  ...
  "level": "INFO",
  "loggerName": "org.demo.serialization.APIGatewayRequestDeserializationFunction",
  "message": "product=Product{id=1234, name='product', price=42.0}\n",
   ...
}
```

### 2. SQS Queue
For the SQS handler, we have to send a request to our queue. We can either construct the Queue URL (see below), or
find it from the SQS section of the AWS console.

```bash
 aws sqs send-message --queue-url "https://sqs.[REGION].amazonaws.com/[ACCOUNT-ID]/sqs-event-deserialization-queue" --message-body '{"id": 1234, "name": "product", "price": 123}'
```

Here we can find the message by filtering through the logs for messages that have come back from our SQS handler:

```bash
sam logs --tail --stack-name $MY_STACK --filter SQS 
```

```bash
 {
 ...
  "level": "INFO",
  "loggerName": "org.demo.serialization.SQSEventDeserializationFunction",
  "message": "products=[Product{id=1234, name='product', price=42.0}]\n",
  ...
}

```
