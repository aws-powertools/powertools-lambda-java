---
title: Validation
description: Utility
---

This utility provides JSON Schema validation for payloads held within events and response used in AWS Lambda.

**Key features**

* Validate incoming events and responses
* Built-in validation for most common events (API Gateway, SNS, SQS, ...)
* JMESPath support validate only a sub part of the event

## Install
Depending on your version of Java (either Java 1.8 or 11+), the configuration slightly changes.

=== "Maven Java 11+"
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
    <build>
        <plugins>
            ...
            <plugin>
                 <groupId>dev.aspectj</groupId>
                 <artifactId>aspectj-maven-plugin</artifactId>
                 <version>1.13.1</version>
                 <configuration>
                     <source>11</source> <!-- or higher -->
                     <target>11</target> <!-- or higher -->
                     <complianceLevel>11</complianceLevel> <!-- or higher -->
                     <aspectLibraries>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-validation</artifactId>
                         </aspectLibrary>
                     </aspectLibraries>
                 </configuration>
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

=== "Maven Java 1.8"

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
    <build>
        <plugins>
            ...
            <plugin>
                 <groupId>org.codehaus.mojo</groupId>
                 <artifactId>aspectj-maven-plugin</artifactId>
                 <version>1.14.0</version>
                 <configuration>
                     <source>1.8</source>
                     <target>1.8</target>
                     <complianceLevel>1.8</complianceLevel>
                     <aspectLibraries>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-validation</artifactId>
                         </aspectLibrary>
                     </aspectLibraries>
                 </configuration>
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

=== "Gradle Java 11+"

    ```groovy hl_lines="3 11"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-validation:{{ powertools.version }}'
        }
        
        sourceCompatibility = 11 // or higher
        targetCompatibility = 11 // or higher
    ```

=== "Gradle Java 1.8"

    ```groovy hl_lines="3 11"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '6.6.3'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-validation:{{ powertools.version }}'
        }
        
        sourceCompatibility = 1.8
        targetCompatibility = 1.8
    ```


## Validating events

You can validate inbound and outbound events using `@Validation` annotation.

You can also use the `Validator#validate()` methods, if you want more control over the validation process such as handling a validation error.

We support JSON schema version 4, 6, 7 and 201909 (from [jmespath-jackson library](https://github.com/burtcorp/jmespath-java)).

### Validation annotation

`@Validation` annotation is used to validate either inbound events or functions' response.

It will fail fast with `ValidationException` if an event or response doesn't conform with given JSON Schema.

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

!!! note
    It is not a requirement to validate both inbound and outbound schemas - You can either use one, or both.

### Validate function

Validate standalone function is used within the Lambda handler, or any other methods that perform data validation.

You can also gracefully handle schema validation errors by catching `ValidationException`.

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

!!! note
    Schemas are stored in memory for re-use, to avoid loading them from file each time.

## Built-in events and responses

For the following events and responses, the Validator will automatically perform validation on the content.

**Events**

 Type of event | Class | Path to content |
 ------------------------------------------------- | ------------------------------------------------- | -------------------------------------------------
 API Gateway REST |  APIGatewayProxyRequestEvent |  `body`
 API Gateway HTTP |  APIGatewayV2HTTPEvent | `body`
 Application Load Balancer |  ApplicationLoadBalancerRequestEvent | `body`
 Cloudformation Custom Resource |  CloudFormationCustomResourceEvent | `resourceProperties`
 CloudWatch Logs |  CloudWatchLogsEvent | `awslogs.powertools_base64_gzip(data)`
 EventBridge / Cloudwatch |  ScheduledEvent | `detail`
 Kafka |  KafkaEvent | `records[*][*].value`
 Kinesis |  KinesisEvent | `Records[*].kinesis.powertools_base64(data)`
 Kinesis Firehose |  KinesisFirehoseEvent | `Records[*].powertools_base64(data)`
 Kinesis Analytics from Firehose |  KinesisAnalyticsFirehoseInputPreprocessingEvent | `Records[*].powertools_base64(data)`
 Kinesis Analytics from Streams |  KinesisAnalyticsStreamsInputPreprocessingEvent | `Records[*].powertools_base64(data)`
 SNS |  SNSEvent | `Records[*].Sns.Message`
 SQS |  SQSEvent | `Records[*].body`

**Responses**

 Type of response | Class | Path to content (envelope)
 ------------------------------------------------- | ------------------------------------------------- | -------------------------------------------------
 API Gateway REST | APIGatewayProxyResponseEvent} | `body`
 API Gateway HTTP | APIGatewayV2HTTPResponse} | `body`
 API Gateway WebSocket | APIGatewayV2WebSocketResponse} | `body`
 Load Balancer | ApplicationLoadBalancerResponseEvent} | `body`
 Kinesis Analytics | KinesisAnalyticsInputPreprocessingResponse} | `Records[*].powertools_base64(data)``

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
By default, powertools-validation is configured with [V7](https://json-schema.org/draft-07/json-schema-release-notes.html).
You can use the `ValidationConfig` to change that behaviour.

=== "Handler with custom schema version"

    ```java hl_lines="6"
    ...
    import software.amazon.lambda.powertools.validation.ValidationConfig;
    import software.amazon.lambda.powertools.validation.Validation;

    static {
        ValidationConfig.get().setSchemaVersion(SpecVersion.VersionFlag.V4);
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
