#  Powertools for AWS Lambda (Java) - Validation Example

This project contains an example of Lambda function using the validation module of Powertools for AWS Lambda (Java). 
For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/validation/).

The handler [InboundValidation](src/main/java/org/demo/validation/InboundValidation.java) validates incoming HTTP requests
received from the API gateway against [schema.json](src/main/resources/schema.json).

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md)

## Test the application

To test the validation, we can POST a JSON object shaped like our schema: 
```bash
 curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/hello/ -H "Content-Type: application/json" -d '{"id": 123,"name":"The Hitchhikers Guide to the Galaxy","price":10.99}'
```

If we break the schema - for instance, by removing one of the compulsory fields, 
we will get an error back from our API and will see a `ValidationException` in the logs:

```bash
 sam logs --tail --stack-name $MY_STACK
```
