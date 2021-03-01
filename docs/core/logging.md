---
title: Logging
description: Core utility
---

Logger provides an opinionated logger with output structured as JSON.

**Key features**

* Capture key fields from Lambda context, cold start and structures logging output as JSON
* Log Lambda event when instructed (disabled by default)
    - Enable explicitly via annotation param
* Append additional keys to structured log at any point in time

## Initialization

Powertools extends the functionality of Log4J. Below is an example log4j2.xml file, with the LambdaJsonLayout configured.

=== "log4j2.xml"

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <Configuration packages="com.amazonaws.services.lambda.runtime.log4j2">
        <Appenders>
            <Console name="JsonAppender" target="SYSTEM_OUT">
                <LambdaJsonLayout compact="true" eventEol="true"/>
            </Console>
        </Appenders>
        <Loggers>
            <Logger name="JsonLogger" level="INFO" additivity="false">
                <AppenderRef ref="JsonAppender"/>
            </Logger>
            <Root level="info">
                <AppenderRef ref="JsonAppender"/>
            </Root>
        </Loggers>
    </Configuration>
    ```

You can also override log level by setting **`POWERTOOLS_LOG_LEVEL`** env var. Here is an example using AWS Serverless Application Model (SAM)

=== "template.yaml"
    ``` yaml hl_lines="9 10"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
            Environment:
                Variables:
                    POWERTOOLS_LOG_LEVEL: DEBUG
                    POWERTOOLS_SERVICE_NAME: example
    ```

You can also explicitly set a service name via **`POWERTOOLS_SERVICE_NAME`** env var. This sets **service** key that will be present across all log statements.

## Standard structured keys

Your Logger will always include the following keys to your structured logging:

Key | Type | Example | Description
------------------------------------------------- | ------------------------------------------------- | --------------------------------------------------------------------------------- | -------------------------------------------------
**timestamp** | String | "2020-05-24 18:17:33,774" | Timestamp of actual log statement
**level** | String | "INFO" | Logging level
**coldStart** | Boolean | true| ColdStart value.
**service** | String | "payment" | Service name defined. "service_undefined" will be used if unknown
**samplingRate** | int |  0.1 | Debug logging sampling rate in percentage e.g. 10% in this case
**message** | String |  "Collecting payment" | Log statement value. Unserializable JSON values will be casted to string
**functionName**| String | "example-powertools-HelloWorldFunction-1P1Z6B39FLU73"
**functionVersion**| String | "12"
**functionMemorySize**| String | "128"
**functionArn**| String | "arn:aws:lambda:eu-west-1:012345678910:function:example-powertools-HelloWorldFunction-1P1Z6B39FLU73"
**xray_trace_id**| String | "1-5759e988-bd862e3fe1be46a994272793" | X-Ray Trace ID when Lambda function has enabled Tracing
**function_request_id**| String | "899856cb-83d1-40d7-8611-9e78f15f32f4"" | AWS Request ID from lambda context

## Capturing context Lambda info

You can enrich your structured logs with key Lambda context information via `logEvent` annotation parameter. 
You can also explicitly log any incoming event using `logEvent` param. Refer [Override default object mapper](#override-default-object-mapper) 
to customise what is logged.

!!! warning
    Log event is disabled by default to prevent sensitive info being logged.


=== "App.java"

    ```java hl_lines="14"
    import org.apache.logging.log4j.LogManager;
    import org.apache.logging.log4j.Logger;
    import software.amazon.lambda.logging.LoggingUtils;
    import software.amazon.lambda.logging.Logging;
    ...
    
    /**
     * Handler for requests to Lambda function.
     */
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        Logger log = LogManager.getLogger();
    
        @Logging
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
         ...
        }
    }
    ```

=== "AppLogEvent.java"
    
    ```java hl_lines="8"
    /**
     * Handler for requests to Lambda function.
     */
    public class AppLogEvent implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        Logger log = LogManager.getLogger();
        
        @Logging(logEvent = true)
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
         ...
        }
    }
    ```

## Appending additional keys

You can append your own keys to your existing Logger via `appendKey`.

=== "App.java"

    ```java hl_lines="11 19"
    /**
     * Handler for requests to Lambda function.
     */
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        Logger log = LogManager.getLogger();
    
        @Logging(logEvent = true)
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
            ...
            LoggingUtils.appendKey("test", "willBeLogged");
            ...
    
            ...
             Map<String, String> customKeys = new HashMap<>();
             customKeys.put("test", "value");
             customKeys.put("test1", "value1");
    
             LoggingUtils.appendKeys(customKeys);
            ...
        }
    }
    ```

## Override default object mapper

You can optionally choose to override default object mapper which is used to serialize lambda function events. You might
want to supply custom object mapper in order to control how serialisation is done, for example, when you want to log only
specific fields from received event due to security.

=== "App.java"

    ```java hl_lines="9 10"
    /**
     * Handler for requests to Lambda function.
     */
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        Logger log = LogManager.getLogger();

        static {
            ObjectMapper objectMapper = new ObjectMapper();
            LoggingUtils.defaultObjectMapper(objectMapper);
        }
    
        @Logging(logEvent = true)
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
            ...
        }
    }
    ```

## Sampling debug logs

You can dynamically set a percentage of your logs to **DEBUG** level via env var `POWERTOOLS_LOGGER_SAMPLE_RATE` or
via `samplingRate` attribute on annotation. 

!!! info
    Configuration on environment variable is given precedence over sampling rate configuration on annotation, provided it's in valid value range.

=== "Sampling via annotation attribute"

    ```java hl_lines="8"
    /**
     * Handler for requests to Lambda function.
     */
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        Logger log = LogManager.getLogger();
    
        @Logging(samplingRate = 0.5)
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
         ...
        }
    }
    ```

=== "Sampling via environment variable"

    ```yaml hl_lines="9"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
            Environment:
                Variables:
                    POWERTOOLS_LOGGER_SAMPLE_RATE: 0.5
    ```