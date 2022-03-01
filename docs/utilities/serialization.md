---
title: Serialization Utilities
description: Utility
---

This module contains a set of utilities you may use in your Lambda functions, to manipulate JSON.

## Easy deserialization

### Key features

* Easily deserialize the main content of an event (for example, the body of an API Gateway event)
* 15+ built-in events (see the [list below](#built-in-events))

### Getting started

=== "Maven"

    ```xml hl_lines="5" 
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-serialization</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        ...
    </dependencies>
    ```

=== "Gradle"

    ```
    implementation 'software.amazon.lambda:powertools-serialization:{{ powertools.version }}'
    ```

### EventDeserializer

The `EventDeserializer` can be used to extract the main part of an event (body, message, records) and deserialize it from JSON to your desired type.

It can handle single elements like the body of an API Gateway event:

=== "APIGWHandler.java"

    ```java hl_lines="1 6 9" 
    import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

    public class APIGWHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(
                final APIGatewayProxyRequestEvent event, 
                final Context context) {

            Product product = extractDataFrom(event).as(Product.class);

        }
    ```

=== "Product.java"

    ```java 
    public class Product {
        private long id;
        private String name;
        private double price;
    
        public Product() {
        }
    
        public Product(long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    
        public long getId() {
            return id;
        }
    
        public void setId(long id) {
            this.id = id;
        }
    
        public String getName() {
            return name;
        }
    
        public void setName(String name) {
            this.name = name;
        }
    
        public double getPrice() {
            return price;
        }
    
        public void setPrice(double price) {
            this.price = price;
        }
    }
    ```

=== "event"

    ```json hl_lines="2"
    {
        "body": "{\"id\":1234, \"name\":\"product\", \"price\":42}",
        "resource": "/{proxy+}",
        "path": "/path/to/resource",
        "httpMethod": "POST",
        "isBase64Encoded": false,
        "queryStringParameters": {
            "foo": "bar"
        },
        "pathParameters": {
            "proxy": "/path/to/resource"
        },
        "stageVariables": {
            "baz": "qux"
        },
        "headers": {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Encoding": "gzip, deflate, sdch",
            "Accept-Language": "en-US,en;q=0.8",
            "Cache-Control": "max-age=0",
            "Host": "1234567890.execute-api.us-east-1.amazonaws.com",
            "Upgrade-Insecure-Requests": "1",
            "User-Agent": "Custom User Agent String",
            "Via": "1.1 08f323deadbeefa7af34d5feb414ce27.cloudfront.net (CloudFront)",
            "X-Amz-Cf-Id": "cDehVQoZnx43VYQb9j2-nvCh-9z396Uhbp027Y2JvkCPNLmGJHqlaA==",
            "X-Forwarded-For": "127.0.0.1, 127.0.0.2",
            "X-Forwarded-Port": "443",
            "X-Forwarded-Proto": "https"
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

It can also handle a collection of elements like the records of an SQS event:

=== "SQSHandler.java"

    ```java hl_lines="1 6 9" 
    import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

    public class SQSHandler implements RequestHandler<SQSEvent, String> {
    
        public String handleRequest(
                final SQSEvent event, 
                final Context context) {

            List<Product> products = extractDataFrom(event).asListOf(Product.class);

        }
    ```

=== "event"
    
    ```json hl_lines="6 23"
    {
        "Records": [
          {
            "messageId": "d9144555-9a4f-4ec3-99a0-34ce359b4b54",
            "receiptHandle": "13e7f7851d2eaa5c01f208ebadbf1e72==",
            "body": "{  \"id\": 1234,  \"name\": \"product\",  \"price\": 42}",
            "attributes": {
                "ApproximateReceiveCount": "1",
                "SentTimestamp": "1601975706495",
                "SenderId": "AROAIFU437PVZ5L2J53F5",
                "ApproximateFirstReceiveTimestamp": "1601975706499"
            },
            "messageAttributes": {
            },
            "md5OfBody": "13e7f7851d2eaa5c01f208ebadbf1e72",
            "eventSource": "aws:sqs",
            "eventSourceARN": "arn:aws:sqs:eu-central-1:123456789012:TestLambda",
            "awsRegion": "eu-central-1"
          },
          {
            "messageId": "d9144555-9a4f-4ec3-99a0-34ce359b4b54",
            "receiptHandle": "13e7f7851d2eaa5c01f208ebadbf1e72==",
            "body": "{  \"id\": 12345,  \"name\": \"product5\",  \"price\": 45}",
            "attributes": {
              "ApproximateReceiveCount": "1",
              "SentTimestamp": "1601975706495",
              "SenderId": "AROAIFU437PVZ5L2J53F5",
              "ApproximateFirstReceiveTimestamp": "1601975706499"
            },
            "messageAttributes": {
              
            },
            "md5OfBody": "13e7f7851d2eaa5c01f208ebadbf1e72",
            "eventSource": "aws:sqs",
            "eventSourceARN": "arn:aws:sqs:eu-central-1:123456789012:TestLambda",
            "awsRegion": "eu-central-1"
          }
        ]
    }
    ```

!!! Tip
    In the background, `EventDeserializer` is using Jackson. The `ObjectMapper` is configured in `JsonConfig`. You can customize the configuration of the mapper if needed:
    `JsonConfig.get().getObjectMapper()`. Using this feature, you don't need to add Jackson to your project and create another instance of `ObjectMapper`.
  
### Built-in events

| Event Type                                        | Path to the content                                       | List |
|---------------------------------------------------|-----------------------------------------------------------|------|
| `APIGatewayProxyRequestEvent`                     | `body`                                                    |      |
| `APIGatewayV2HTTPEvent`                           | `body`                                                    |      |
| `SNSEvent`                                        | `Records[0].Sns.Message`                                  |      |
| `SQSEvent`                                        | `Records[*].body`                                         | x    | 
 | `ScheduledEvent`                                  | `detail`                                                  |      |
 | `ApplicationLoadBalancerRequestEvent`             | `body`                                                    |      | 
 | `CloudWatchLogsEvent`                             | `powertools_base64_gzip(data)`                            |      | 
 | `CloudFormationCustomResourceEvent`               | `resourceProperties`                                      |      | 
 | `KinesisEvent`                                    | `Records[*].kinesis.powertools_base64(data)`              | x    | 
 | `KinesisFirehoseEvent`                            | `Records[*].powertools_base64(data)`                      | x    | 
 | `KafkaEvent`                                      | `records[*].values[*].powertools_base64(value)`           | x    | 
 | `ActiveMQEvent`                                   | `messages[*].powertools_base64(data)`                     | x    | 
| `RabbitMQEvent`                                   | `rmqMessagesByQueue[*].values[*].powertools_base64(data)` | x    | 
| `KinesisAnalyticsFirehoseInputPreprocessingEvent` | `Records[*].kinesis.powertools_base64(data)`              | x    | 
| `KinesisAnalyticsStreamsInputPreprocessingEvent`  | `Records[*].kinesis.powertools_base64(data)`              | x    | 


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
