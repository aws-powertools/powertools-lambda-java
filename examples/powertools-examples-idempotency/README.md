# Lambda Powertools for Java -  Idempotency Example

This project contains an example of Lambda function using the idempotency module of Powertools for AWS Lambda (Java). For more information on this module, please refer to the [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/idempotency/).
The example exposes a HTTP POST endpoint. When the user sends the address of a webpage to it, the endpoint fetches the contents of the URL and returns them to the user:

## Deploy the sample application

This sample is based on Serverless Application Model (SAM). To deploy it, check out the instructions for getting
started with SAM in [the examples directory](../README.md)

## Test the application

```bash
 curl -X POST https://[REST-API-ID].execute-api.[REGION].amazonaws.com/Prod/helloidem/ -H "Content-Type: application/json" -d '{"address": "https://checkip.amazonaws.com"}'
```

this should return the contents of the webpage, for instance:
```json
{ "message": "hello world", "location": "123.123.123.1" }
```

Check out [App.java](src/main/java/helloworld/App.java) to see how it works!
