---
title: Tracing
description: Core utility
---

Powertools tracing is an opinionated thin wrapper for [AWS X-Ray Java SDK](https://github.com/aws/aws-xray-sdk-java/)
a provides functionality to reduce the overhead of performing common tracing tasks.

![Tracing showcase](../media/tracing_utility_showcase.png)

 **Key Features**

 * Capture cold start as annotation, and responses as well as full exceptions as metadata
 * Helper methods to improve the developer experience of creating new X-Ray subsegments.
 * Better developer experience when developing with multiple threads.
 * Auto patch supported modules by AWS X-Ray
 * GraalVM support

## Install

=== "Maven"

    ```xml hl_lines="3-7 16 18 24-27"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-tracing</artifactId>
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
                 <version>1.14</version>
                 <configuration>
                     <source>11</source> <!-- or higher -->
                     <target>11</target> <!-- or higher -->
                     <complianceLevel>11</complianceLevel> <!-- or higher -->
                     <aspectLibraries>
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
                             <artifactId>powertools-tracing</artifactId>
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

    ```groovy hl_lines="3 11"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-tracing:{{ powertools.version }}'
        }
        
        sourceCompatibility = 11
        targetCompatibility = 11
    ```

## Initialization

Before your use this utility, your AWS Lambda function [must have permissions](https://docs.aws.amazon.com/lambda/latest/dg/services-xray.html#services-xray-permissions) to send traces to AWS X-Ray.

> Example using AWS Serverless Application Model (SAM)

=== "template.yaml"

    ```yaml hl_lines="8 11"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
    
            Tracing: Active
            Environment:
                Variables:
                    POWERTOOLS_SERVICE_NAME: example
    ```

The Powertools for AWS Lambda (Java) service name is used as the X-Ray namespace. This can be set using the environment variable
`POWERTOOLS_SERVICE_NAME`

### Lambda handler

To enable Powertools for AWS Lambda (Java) tracing to your function add the `@Tracing` annotation to your `handleRequest` method or on
any method will capture the method as a separate subsegment automatically. You can optionally choose to customize 
segment name that appears in traces.

=== "Tracing annotation"

    ```java hl_lines="3 10 15"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            businessLogic1();
    
            businessLogic2();
        }
    
        @Tracing
        public void businessLogic1(){
    
        }
    
        @Tracing
        public void businessLogic2(){
    
        }
    }
    ```

=== "Custom Segment names"

    ```java hl_lines="3"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing(segmentName="yourCustomName")
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ...
        }
    ```

When using this `@Tracing` annotation, Utility performs these additional tasks to ease operations:

  * Creates a `ColdStart` annotation to easily filter traces that have had an initialization overhead.
  * Creates a `Service` annotation if service parameter or `POWERTOOLS_SERVICE_NAME` is set.
  * Captures any response, or full exceptions generated by the handler, and include as tracing metadata.


By default, this annotation will automatically record method responses and exceptions. You can change the default behavior by setting
the environment variables `POWERTOOLS_TRACER_CAPTURE_RESPONSE` and `POWERTOOLS_TRACER_CAPTURE_ERROR` as needed. Optionally, you can override behavior by
different supported `captureMode` to record response, exception or both.

!!! warning "Returning sensitive information from your Lambda handler or functions, where `Tracing` is used?"
    You can disable annotation from capturing their responses and exception as tracing metadata with **`captureMode=DISABLED`**
    or globally by setting environment variables **`POWERTOOLS_TRACER_CAPTURE_RESPONSE`** and **`POWERTOOLS_TRACER_CAPTURE_ERROR`** to **`false`**

=== "Disable on annotation"

    ```java hl_lines="3"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing(captureMode=CaptureMode.DISABLED)
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ...
        }
    ```

=== "Disable Globally"

    ```yaml hl_lines="11 12"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
    
            Tracing: Active
            Environment:
                Variables:
                    POWERTOOLS_TRACER_CAPTURE_RESPONSE: false
                    POWERTOOLS_TRACER_CAPTURE_ERROR: false
    ```

### Annotations & Metadata

**Annotations** are key-values associated with traces and indexed by AWS X-Ray. You can use them to filter traces and to
create [Trace Groups](https://aws.amazon.com/about-aws/whats-new/2018/11/aws-xray-adds-the-ability-to-group-traces/) to slice and dice your transactions.

**Metadata** are key-values also associated with traces but not indexed by AWS X-Ray. You can use them to add additional 
context for an operation using any native object.

=== "Annotations"

    You can add annotations using `putAnnotation()` method from TracingUtils
    ```java hl_lines="8"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.putAnnotation("annotation", "value");
        }
    }
    ```

=== "Metadata"

    You can add metadata using `putMetadata()` method from TracingUtils
    ```java hl_lines="8"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.putMetadata("content", "value");
        }
    }
    ```

## Override default object mapper

You can optionally choose to override default object mapper which is used to serialize method response and exceptions when enabled. You might
want to supply custom object mapper in order to control how serialisation is done, for example, when you want to log only
specific fields from received event due to security.

=== "App.java"

    ```java hl_lines="10-14"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;
    import static software.amazon.lambda.powertools.tracing.CaptureMode.RESPONSE;

    /**
     * Handler for requests to Lambda function.
     */
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> { 
        static {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule simpleModule = new SimpleModule();
            objectMapper.registerModule(simpleModule);

            TracingUtils.defaultObjectMapper(objectMapper);
        }
    
        @Tracing(captureMode = RESPONSE)
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
            ...
        }
    }
    ```

## Utilities

Tracing modules comes with certain utility method when you don't want to use annotation for capturing a code block
under a subsegment, or you are doing multithreaded programming. Refer examples below.

=== "Functional Api"

    ```java hl_lines="7 8 9 11 12 13"
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;
    
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
             TracingUtils.withSubsegment("loggingResponse", subsegment -> {
                // Some business logic
             });
    
             TracingUtils.withSubsegment("localNamespace", "loggingResponse", subsegment -> {
                // Some business logic
             });
        }
    }
    ```

=== "Multi Threaded Programming"

    ```java hl_lines="7 9 10 11"
    import static software.amazon.lambda.powertools.tracing.TracingUtils.withEntitySubsegment;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Extract existing trace data
            Entity traceEntity = AWSXRay.getTraceEntity();
    
            Thread anotherThread = new Thread(() -> withEntitySubsegment("inlineLog", traceEntity, subsegment -> {
                // Business logic in separate thread
            }));
        }
    }
    ```

## Instrumenting SDK clients and HTTP calls

Powertools for Lambda (Java) cannot intercept SDK clients instantiation to add X-Ray instrumentation. You should make sure to instrument the SDK clients explicitly. Refer details on
[how to instrument SDK client with Xray](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html#xray-sdk-java-awssdkclients) 
and [outgoing http calls](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html#xray-sdk-java-httpclients). For example:

=== "LambdaHandler.java"

    ```java hl_lines="1 2 7"
    import com.amazonaws.xray.AWSXRay;
    import com.amazonaws.xray.handlers.TracingHandler;

    public class LambdaHandler {
        private AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withRegion(Regions.fromName(System.getenv("AWS_REGION")))
            .withRequestHandlers(new TracingHandler(AWSXRay.getGlobalRecorder()))
            .build();
        // ...
    }
    ```

## Testing your code

When using `@Tracing` annotation, your Junit test cases needs to be configured to create parent Segment required by [AWS X-Ray SDK for Java](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html).

Below are two ways in which you can configure your tests.

#### Configure environment variable on project level (Recommended)

You can choose to configure environment variable on project level for your test cases run. This is recommended approach as it will avoid the need of configuring each test case specifically.

Below are examples configuring your maven/gradle projects. You can choose to configure it differently as well as long as you are making sure that environment variable `LAMBDA_TASK_ROOT` is set. This variable is 
used internally via AWS X-Ray SDK to configure itself properly for lambda runtime.

=== "Maven (pom.xml)"

    ```xml hl_lines="4-13"
    <build>
        ...
      <plugins>
        <!--  Configures environment variable to avoid initialization of AWS X-Ray segments for each tests-->
          <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-surefire-plugin</artifactId>
              <configuration>
                  <environmentVariables>
                      <LAMBDA_TASK_ROOT>handler</LAMBDA_TASK_ROOT>
                  </environmentVariables>
              </configuration>
          </plugin>
      </plugins>
    </build>
    
    ```

=== "Gradle (build.gradle) "
    
    ```json hl_lines="2-4"
    // Configures environment variable to avoid initialization of AWS X-Ray segments for each tests
    test {
        environment "LAMBDA_TASK_ROOT", "handler"
    }
    ```

#### Configure test cases (Not Recommended)

You can choose to configure each of your test case instead as well if you choose not to configure environment variable on project level. 
Below is an example configuration needed for each test case.

=== "AppTest.java"

    ```java hl_lines="10 11 12 17 18 19 20 21 22 23 24"
    import com.amazonaws.xray.AWSXRay;
    import org.junit.After;
    import org.junit.Before;
    import org.junit.Test;

    public class AppTest {

        @Before
        public void setup() {
            if(null == System.getenv("LAMBDA_TASK_ROOT")) {
                AWSXRay.beginSegment("test");
            }
        }
        
        @After
        public void tearDown() {
            // Needed when using sam build --use-container
            if (AWSXRay.getCurrentSubsegmentOptional().isPresent()) {
                AWSXRay.endSubsegment();
            }

            if(null == System.getenv("LAMBDA_TASK_ROOT")) {
              AWSXRay.endSegment();
            }
        }

        @Test
        public void successfulResponse() {
            // test logic
        }
    ```



