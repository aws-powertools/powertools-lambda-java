# Deserialization

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

