# Lambda Powertools for Java - Serialization Example

This project contains an example of Lambda function using the serialization utilities module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/serialization/).

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
started with SAM in [the examples directory](../README.md)

## Test the application

### 1. API Gateway Endpoint

To test the HTTP endpoint, we can post a product to the test URL:

```bash
curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/product/ -H "Content-Type: application/json" -d '{"id": 1234, "name": "product", "price": 42}'
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
 aws sqs send-message --queue-url "https://sqs.[REGION].amazonaws.com/[ACCOUNT-ID]/sqs-event-deserialization-queue" --message-body '{"id": 1234, "name": "product", "price"
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
