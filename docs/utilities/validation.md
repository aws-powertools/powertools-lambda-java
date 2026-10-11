---
title: Validation
description: Utility
---

This utility provides JSON Schema validation for payloads held within events and response used in AWS Lambda.

**Key features**

* Validate incoming events and responses
* Built-in validation for most common events (API Gateway, SNS, SQS, ...) and support for partial batch failures (SQS, Kinesis)
* JMESPath support validate only a sub part of the event

## Install

=== "Maven"
    ```xml hl_lines="3-7 16 18 24-27"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-validation</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        ...
    </dependencies>
    ...
    <!-- configure the aspectj-maven-plugin to compile-time weave (CTW) the aws-lambda-powertools-java aspects into your project -->
    <!-- Note: This AspectJ configuration is not needed when using the functional approach with ValidationUtils.validate() -->
    <build>
        <plugins>
            ...
            <plugin>
                 <groupId>dev.aspectj</groupId>
                 <artifactId>aspectj-maven-plugin</artifactId>
                 <version>1.14</version>
                 <configuration>
                     <source>17</source> <!-- or higher -->
                     <target>17</target> <!-- or higher -->
                     <complianceLevel>17</complianceLevel> <!-- or higher -->
                     <aspectLibraries>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-validation</artifactId>
                         </aspectLibrary>
                     </aspectLibraries>
                 </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.aspectj</groupId>
                        <artifactId>aspectjtools</artifactId>
                        <!-- AspectJ compiler version, in sync with runtime -->
                        <version>1.9.22</version>
                    </dependency>
                </dependencies>
                 <executions>
                     <execution>
                         <goals>
                             <goal>compile</goal>
                         </goals>
                     </execution>
                 </executions>
            </plugin>
            ...
        </plugins>
    </build>
    ```

=== "Gradle"

    ```groovy hl_lines="3 11 12"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0' // Not needed when using the functional approach with ValidationUtils.validate()
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-validation:{{ powertools.version }}' // Not needed when using the functional approach with ValidationUtils.validate()
            implementation 'software.amazon.lambda:powertools-validation:{{ powertools.version }}' // Use this instead of 'aspect' when using the functional approach
        }
        
        sourceCompatibility = 17 // or higher
        targetCompatibility = 17 // or higher
    ```

## Validating events

You can validate inbound and outbound events using either the `@Validation` annotation or the functional approach with `ValidationUtils.validate()` methods:

- **@Validation annotation** - Simpler syntax with automatic validation, but requires AspectJ configuration
- **ValidationUtils.validate()** - No AspectJ required, provides more control over the validation process such as handling validation errors

We support JSON schema version 4, 6, 7, 2019-09 and 2020-12 using the [NetworkNT JSON Schema Validator](https://github.com/networknt/json-schema-validator) ([Compatibility with JSON Schema versions](https://github.com/networknt/json-schema-validator/blob/master/doc/compatibility.md)).

The validator is configured to enable format assertions by default even for 2019-09 and 2020-12. Schemas referenced with a remote `$ref` (for example `https://example.com/schema.json`) are fetched over the network.

!!! warning "Powertools now uses NetworkNT JSON Schema Validator 2.x"
    This changes some types in the public API and the format of validation errors. See [Migrating to json-schema-validator 2.x](#migrating-to-json-schema-validator-2x).

When validation fails, the `ValidationException` message contains the validation errors serialized as JSON. See [Validation error format](#validation-error-format) for an example.

### Validation annotation

The `@Validation` annotation is used to validate either inbound events or functions' response.

It will fail fast if an event or response doesn't conform with given JSON Schema. For most type of events a `ValidationException` will be thrown.

For API gateway events associated with REST APIs and HTTP APIs -  `APIGatewayProxyRequestEvent` and `APIGatewayV2HTTPEvent` - the `@Validation` 
annotation will build and return a custom 400 / "Bad Request" response, with a body containing the validation errors. This saves you from having
to catch the validation exception and map it back to a meaningful user error yourself.

For SQS and Kinesis events - `SQSEvent` and `KinesisEvent`- the `@Validation` annotation will add the invalid messages
to the batch item failures list in the response, respectively `SQSBatchResponse` and `StreamsEventResponse` 
and removed from the event so that you do not process them within the handler. 

While it is easier to specify a json schema file in the classpath (using the notation `"classpath:/path/to/schema.json"`), you can also provide a JSON String containing the schema.

=== "MyFunctionHandler.java"

    ```java hl_lines="6"
    import software.amazon.lambda.powertools.validation.Validation;

    public class MyFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Override
        @Validation(inboundSchema = "classpath:/schema_in.json", outboundSchema = "classpath:/schema_out.json")
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // ...
            return something;
        }
    }
    ```

**NOTE**: It's not a requirement to validate both inbound and outbound schemas - You can either use one, or both.

### Functional approach with ValidationUtils

The `ValidationUtils.validate()` method provides a functional approach that can be used within the Lambda handler or any other methods that perform data validation. This approach does not require AspectJ configuration.

With this approach, you can gracefully handle schema validation errors by catching `ValidationException`.

=== "MyFunctionHandler.java"

    ```java hl_lines="8"
    import static software.amazon.lambda.powertools.validation.ValidationUtils.*;
    
    public class MyFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Override
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            try {
                validate(input, "classpath:/schema.json");
            } catch (ValidationException ex) {
                // do something before throwing it
                throw ex;
            }
    
            // ...
            return something;
        }
    }
    ```

**NOTE**: Schemas are stored in memory for reuse, to avoid loading them from file each time.

## Built-in events and responses

For the following events and responses, the Validator will automatically perform validation on the content.

**Events**

| Type of event                   | Class                                           | Path to content                              |
|---------------------------------|-------------------------------------------------|----------------------------------------------|
| API Gateway REST                | APIGatewayProxyRequestEvent                     | `body`                                       |
| API Gateway HTTP                | APIGatewayV2HTTPEvent                           | `body`                                       |
| Application Load Balancer       | ApplicationLoadBalancerRequestEvent             | `body`                                       |
| Cloudformation Custom Resource  | CloudFormationCustomResourceEvent               | `resourceProperties`                         |
| CloudWatch Logs                 | CloudWatchLogsEvent                             | `awslogs.powertools_base64_gzip(data)`       |
| EventBridge / Cloudwatch        | ScheduledEvent                                  | `detail`                                     |
| Kafka                           | KafkaEvent                                      | `records[*][*].value`                        |
| Kinesis                         | KinesisEvent                                    | `Records[*].kinesis.powertools_base64(data)` |
| Kinesis Firehose                | KinesisFirehoseEvent                            | `Records[*].powertools_base64(data)`         |
| Kinesis Analytics from Firehose | KinesisAnalyticsFirehoseInputPreprocessingEvent | `Records[*].powertools_base64(data)`         |
| Kinesis Analytics from Streams  | KinesisAnalyticsStreamsInputPreprocessingEvent  | `Records[*].powertools_base64(data)`         |
| SNS                             | SNSEvent                                        | `Records[*].Sns.Message`                     |
| SQS                             | SQSEvent                                        | `Records[*].body`                            |

**Responses**

| Type of response      | Class                                       | Path to content (envelope)            |
|-----------------------|---------------------------------------------|---------------------------------------|
| API Gateway REST      | APIGatewayProxyResponseEvent}               | `body`                                |
| API Gateway HTTP      | APIGatewayV2HTTPResponse}                   | `body`                                |
| API Gateway WebSocket | APIGatewayV2WebSocketResponse}              | `body`                                |
| Load Balancer         | ApplicationLoadBalancerResponseEvent}       | `body`                                |
| Kinesis Analytics     | KinesisAnalyticsInputPreprocessingResponse} | `Records[*].powertools_base64(data)` |

## Custom events and responses

You can also validate any Event or Response type, once you have the appropriate schema.

Sometimes, you might want to validate only a portion of it - This is where the envelope parameter is for.

Envelopes are [JMESPath expressions](https://jmespath.org/tutorial.html) to extract a portion of JSON you want before applying JSON Schema validation.

=== "MyCustomEventHandler.java"

    ```java hl_lines="6 7"
    import software.amazon.lambda.powertools.validation.Validation;

    public class MyCustomEventHandler implements RequestHandler<MyCustomEvent, String> {
    
        @Override
        @Validation(inboundSchema = "classpath:/my_custom_event_schema.json",
                    envelope = "basket.products[*]")
        public String handleRequest(MyCustomEvent input, Context context) {
            return "OK";
        }
    }
    ```

=== "my_custom_event_schema.json"

    ```json
    {
      "basket": {
        "products" : [
          {
            "id": 43242,
            "name": "FooBar XY",
            "price": 258
          },
          {
            "id": 765,
            "name": "BarBaz AB",
            "price": 43.99
          }
        ]
      }
    }
    ```

This is quite powerful because you can use JMESPath Query language to extract records from
[arrays, slice and dice](https://jmespath.org/tutorial.html#list-and-slice-projections),
to [pipe expressions](https://jmespath.org/tutorial.html#pipe-expressions)
and [function](https://jmespath.org/tutorial.html#functions) expressions, where you'd extract what you need before validating the actual payload.


## Change the schema version
By default, powertools-validation is configured to use [draft 7](https://json-schema.org/draft-07/json-schema-release-notes.html) (`SpecificationVersion.DRAFT_7`) as the default dialect if [`$schema`](https://json-schema.org/understanding-json-schema/reference/schema#schema) is not explicitly specified within the schema. If [`$schema`](https://json-schema.org/understanding-json-schema/reference/schema#schema) is explicitly specified within the schema, the validator will use the specified dialect.

You can use the `ValidationConfig` to change that behaviour. If `@Validation` sets a `schemaVersion` other than the default `DRAFT_7`, the annotation version takes precedence.

=== "Handler with custom schema version"

    ```java hl_lines="7"
    ...
    import com.networknt.schema.SpecificationVersion;
    import software.amazon.lambda.powertools.validation.ValidationConfig;
    import software.amazon.lambda.powertools.validation.Validation;

    static {
        ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_4);
    }

    public class MyXMLEventHandler implements RequestHandler<MyEventWithXML, String> {
    
        @Override
        @Validation(inboundSchema="classpath:/schema.json", envelope="powertools_xml(path.to.xml_data)")
        public String handleRequest(MyEventWithXML myEvent, Context context) {
            return "OK";
       }
    }
    ```

## Advanced ObjectMapper settings
If you need to configure the Jackson ObjectMapper, you can use the `ValidationConfig`:

=== "Handler with custom ObjectMapper"

    ```java hl_lines="6 7"
    ...
    import software.amazon.lambda.powertools.validation.ValidationConfig;
    import software.amazon.lambda.powertools.validation.Validation;

    static {
        ObjectMapper objectMapper= ValidationConfig.get().getObjectMapper();
        // update (de)serializationConfig or other properties
    }

    public class MyXMLEventHandler implements RequestHandler<MyEventWithXML, String> {
    
        @Override
        @Validation(inboundSchema="classpath:/schema.json", envelope="powertools_xml(path.to.xml_data)")
        public String handleRequest(MyEventWithXML myEvent, Context context) {
            return "OK";
       }
    }
    ```

## Migrating to json-schema-validator 2.x

Powertools upgraded [NetworkNT JSON Schema Validator](https://github.com/networknt/json-schema-validator){target="_blank"} from 1.x to 2.x, because upstream no longer releases 1.x. Version 2.0.0 renamed most public types ([upstream migration guide](https://github.com/networknt/json-schema-validator/blob/master/doc/migration-2.0.0.md){target="_blank"}). This change ships in version 2.12.0, in line with our [dependency lifecycle policy](https://docs.aws.amazon.com/powertools/java/latest/processes/versioning/#dependency-lifecycle){target="_blank"}.

You need to change your code only if you:

- reference `com.networknt.schema` types directly, or set `@Validation(schemaVersion = ...)`;
- call `ValidationConfig.get().getSchemaVersion()`, `setSchemaVersion(...)` or `getFactory()`;
- call `ValidationUtils.getJsonSchema(...)` or a `ValidationUtils.validate(...)` overload that takes a schema;
- read `ValidationUtils.ValidationErrors` or catch `JsonSchemaException`;
- parse the `ValidationException` message or the API Gateway 400 response body.

Everyone else needs no changes.

### Renamed types

| Before (1.x)                                      | After (2.x)                                               |
| ------------------------------------------------- | --------------------------------------------------------- |
| `SpecVersion.VersionFlag`                         | `SpecificationVersion`                                    |
| `V4`, `V6`, `V7`, `V201909`, `V202012`            | `DRAFT_4`, `DRAFT_6`, `DRAFT_7`, `DRAFT_2019_09`, `DRAFT_2020_12` |
| `JsonSchemaFactory` (`ValidationConfig.getFactory()`) | `SchemaRegistry`                                      |
| `JsonSchema`                                      | `Schema`                                                  |
| `Set<ValidationMessage>` (`ValidationErrors.getValidationErrors()`) | `List<Error>`                           |
| `JsonSchemaException`                             | `SchemaException`                                         |

=== "Before"

    ```java hl_lines="1 5 10"
    import com.networknt.schema.SpecVersion;

    public class MyFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        static {
            ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V4);
        }

        @Override
        @Validation(inboundSchema = "classpath:/schema_in.json",
                    schemaVersion = SpecVersion.VersionFlag.V202012)
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // ...
        }
    }
    ```

=== "After"

    ```java hl_lines="1 5 10"
    import com.networknt.schema.SpecificationVersion;

    public class MyFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        static {
            ValidationConfig.get().setSchemaVersion(SpecificationVersion.DRAFT_4);
        }

        @Override
        @Validation(inboundSchema = "classpath:/schema_in.json",
                    schemaVersion = SpecificationVersion.DRAFT_2020_12)
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // ...
        }
    }
    ```

### Validation error format

The `ValidationException` message is also the body of the 400 response that `@Validation` returns for API Gateway events. In 2.x, `keyword` replaces the `type` and `code` fields, and `message` no longer starts with the instance location. The location is still available in `instanceLocation`.

=== "Before"

    ```json hl_lines="4-6"
    {
      "validationErrors": [
        {
          "type": "exclusiveMinimum",
          "code": "1039",
          "message": "/price: must have an exclusive minimum value of 0",
          "instanceLocation": "/price",
          "evaluationPath": "/properties/price/exclusiveMinimum",
          "schemaLocation": "http://example.com/product.json#/properties/price/exclusiveMinimum",
          "messageKey": "exclusiveMinimum",
          "arguments": ["0"]
        }
      ]
    }
    ```

=== "After"

    ```json hl_lines="4 6"
    {
      "validationErrors": [
        {
          "keyword": "exclusiveMinimum",
          "instanceLocation": "/price",
          "message": "must have an exclusive minimum value of 0",
          "evaluationPath": "/properties/price/exclusiveMinimum",
          "schemaLocation": "http://example.com/product.json#/properties/price/exclusiveMinimum",
          "messageKey": "exclusiveMinimum",
          "arguments": ["0"]
        }
      ]
    }
    ```

### Unchanged behavior

Powertools keeps the 1.x defaults where 2.x changed them:

- Draft 7 remains the default schema version.
- Format assertions remain enabled, including for 2019-09 and 2020-12.
- Remote `$ref` fetching remains enabled.

## Advanced

### Lambda SnapStart priming

The Validation utility integrates with AWS Lambda SnapStart to improve restore durations. To make sure the SnapStart priming logic of this utility runs correctly, you need an explicit reference to `ValidationConfig` in your code to allow the library to register before SnapStart takes a memory snapshot. Learn more about what priming is in this [blog post](https://aws.amazon.com/blogs/compute/optimizing-cold-start-performance-of-aws-lambda-using-advanced-priming-strategies-with-snapstart/){target="_blank"}.

If you don't set a custom `ValidationConfig` in your code yet, make sure to reference `ValidationConfig` in your Lambda handler initialization code. This can be done by adding one of the following lines to your handler class:

=== "Constructor"

    ```java hl_lines="7"
    import software.amazon.lambda.powertools.validation.Validation;
    import software.amazon.lambda.powertools.validation.ValidationConfig;

    public class MyFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

        public MyFunctionHandler() {
            ValidationConfig.get(); // Ensure ValidationConfig is loaded for SnapStart
        }

        @Override
        @Validation(inboundSchema = "classpath:/schema_in.json", outboundSchema = "classpath:/schema_out.json")
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // ...
            return something;
        }
    }
    ```

=== "Static Initializer"

    ```java hl_lines="7"
    import software.amazon.lambda.powertools.validation.Validation;
    import software.amazon.lambda.powertools.validation.ValidationConfig;

    public class MyFunctionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

        static {
            ValidationConfig.get(); // Ensure ValidationConfig is loaded for SnapStart
        }

        @Override
        @Validation(inboundSchema = "classpath:/schema_in.json", outboundSchema = "classpath:/schema_out.json")
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // ...
            return something;
        }
    }
    ```
