---
title: Serialization Utilities
description: Utility
---

This module contains a set of utilities you may use in your Lambda functions, mainly associated with other modules like [validation](validation.md) and [idempotency](idempotency.md), to manipulate JSON.

## JMESPath functions

!!! Tip
    [JMESPath](https://jmespath.org/){target="_blank"} is a query language for JSON used by AWS CLI and AWS Lambda Powertools for Java to get a specific part of a json.

### Key features

* Deserialize JSON from JSON strings, base64, and compressed data
* Use JMESPath to extract and combine data recursively

### Getting started

You might have events that contain encoded JSON payloads as string, base64, or even in compressed format. It is a common use case to decode and extract them partially or fully as part of your Lambda function invocation.

You will generally use this in combination with other Lambda Powertools modules ([validation](validation.md) and [idempotency](idempotency.md)) where you might need to extract a portion of your data before using them.

### Built-in functions

Powertools provides the following JMESPath Functions to easily deserialize common encoded JSON payloads in Lambda functions:

#### powertools_json function

Use `powertools_json` function to decode any JSON string anywhere a JMESPath expression is allowed.

Below example use this function to load the content from the body of an API Gateway request event as a JSON object and retrieve the id field in it:

=== "MyHandler.java"

    ```java hl_lines="7"
    public class MyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
      public MyHandler() {
        Idempotency.config()
        .withConfig(
            IdempotencyConfig.builder()
              .withEventKeyJMESPath("powertools_json(body).id")
              .build())
        .withPersistenceStore(
            DynamoDBPersistenceStore.builder()
              .withTableName(System.getenv("TABLE_NAME"))
              .build())
        .configure();
    }
    
    @Idempotent
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
    }
    ```

=== "event"

    ```json hl_lines="2"
    {
      "body": "{\"message\": \"Lambda rocks\", \"id\": 43876123454654}",
      "resource": "/{proxy+}",
      "path": "/path/to/resource",
      "httpMethod": "POST",
      "queryStringParameters": {
        "foo": "bar"
      },
      "headers": {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Encoding": "gzip, deflate, sdch",
        "Accept-Language": "en-US,en;q=0.8",
        "Cache-Control": "max-age=0",
      },
      "requestContext": {
        "accountId": "123456789012",
        "resourceId": "123456",
        "stage": "prod",
        "requestId": "c6af9ac6-7b61-11e6-9a41-93e8deadbeef",
        "requestTime": "09/Apr/2015:12:34:56 +0000",
        "requestTimeEpoch": 1428582896000,
        "identity": {
          "cognitoIdentityPoolId": null,
          "accountId": null,
          "cognitoIdentityId": null,
          "caller": null,
          "accessKey": null,
          "sourceIp": "127.0.0.1",
          "cognitoAuthenticationType": null,
          "cognitoAuthenticationProvider": null,
          "userArn": null,
          "userAgent": "Custom User Agent String",
          "user": null
        },
        "path": "/prod/path/to/resource",
        "resourcePath": "/{proxy+}",
        "httpMethod": "POST",
        "apiId": "1234567890",
        "protocol": "HTTP/1.1"
      }
    }
    ```

#### powertools_base64 function

Use `powertools_base64` function to decode any base64 data.

Below sample will decode the base64 value within the `data` key, and decode the JSON string into a valid JSON before we can validate it.

=== "MyEventHandler.java"

    ```java hl_lines="7"
    import software.amazon.lambda.powertools.validation.ValidationUtils;

    public class MyEventHandler implements RequestHandler<MyEvent, String> {
    
        @Override
        public String handleRequest(MyEvent myEvent, Context context) {
            validate(myEvent, "classpath:/schema.json", "powertools_base64(data)");
            return "OK";
       }
    }
    ```
=== "schema.json"
```json
{
"data" : "ewogICJpZCI6IDQzMjQyLAogICJuYW1lIjogIkZvb0JhciBYWSIsCiAgInByaWNlIjogMjU4Cn0="
}
```

#### powertools_base64_gzip function

Use `powertools_base64_gzip` function to decompress and decode base64 data.

Below sample will decompress and decode base64 data.

=== "MyEventHandler.java"

    ```java hl_lines="7"
    import software.amazon.lambda.powertools.validation.ValidationUtils;

    public class MyEventHandler implements RequestHandler<MyEvent, String> {
    
        @Override
        public String handleRequest(MyEvent myEvent, Context context) {
            validate(myEvent, "classpath:/schema.json", "powertools_base64_gzip(data)");
            return "OK";
       }
    }
    ```

=== "schema.json"

    ```json
    {
       "data" : "H4sIAAAAAAAA/6vmUlBQykxRslIwMTYyMdIBcfMSc1OBAkpu+flOiUUKEZFKYOGCosxkkLiRqQVXLQDnWo6bOAAAAA=="
    }
    ```


### Bring your own JMESPath function

!!! warning
    This should only be used for advanced use cases where you have special formats not covered by the built-in functions.
    Please open an issue in Github if you need us to add some common functions.

Your function must extend `io.burt.jmespath.function.BaseFunction`, take a String as parameter and return a String.
You can read the [doc](https://github.com/burtcorp/jmespath-java#adding-custom-functions) for more information.

Below is an example that takes some xml and transform it into json. Once your function is created, you need to add it
to powertools.You can then use it to do your validation or in idempotency module.

=== "XMLFunction.java"

    ```java
    public class XMLFunction extends BaseFunction {
        public Base64Function() {
            super("powertools_xml", ArgumentConstraints.typeOf(JmesPathType.STRING));
        }
    
        @Override
        protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
            T value = arguments.get(0).value();
            String xmlString = runtime.toString(value);
    
            String jsonString =  // ... transform xmlString to json
    
            return runtime.createString(jsonString);
        }
    }
    ```

=== "Handler with validation API"

    ```java hl_lines="6 13"
    ...
    import software.amazon.lambda.powertools.validation.ValidationConfig;
    import software.amazon.lambda.powertools.validation.ValidationUtils.validate;

    static {
        JsonConfig.get().addFunction(new XMLFunction());
    }

    public class MyXMLEventHandler implements RequestHandler<MyEventWithXML, String> {
    
        @Override
        public String handleRequest(MyEventWithXML myEvent, Context context) {
            validate(myEvent, "classpath:/schema.json", "powertools_xml(path.to.xml_data)");
            return "OK";
       }
    }
    ```

=== "Handler with validation annotation"

    ```java hl_lines="6 12"
    ...
    import software.amazon.lambda.powertools.validation.ValidationConfig;
    import software.amazon.lambda.powertools.validation.Validation;

    static {
        JsonConfig.get().addFunction(new XMLFunction());
    }

    public class MyXMLEventHandler implements RequestHandler<MyEventWithXML, String> {
    
        @Override
        @Validation(inboundSchema="classpath:/schema.json", envelope="powertools_xml(path.to.xml_data)")
        public String handleRequest(MyEventWithXML myEvent, Context context) {
            return "OK";
       }
    }
    ```
