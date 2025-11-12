---
title: Tracing
description: Core utility
---

The Tracing utility is an opinionated thin wrapper for [AWS X-Ray Java SDK](https://github.com/aws/aws-xray-sdk-java/)
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

    ```xml hl_lines="3-7 25-28"
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
    <!-- Note: This AspectJ configuration is not needed when using the functional approach -->
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

    ```groovy hl_lines="3 11 12"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0' // Not needed when using the functional approach
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            aspect 'software.amazon.lambda:powertools-tracing:{{ powertools.version }}' // Not needed when using the functional approach
            implementation 'software.amazon.lambda:powertools-tracing:{{ powertools.version }}' // Use this instead of 'aspect' when using the functional approach
        }
        
        sourceCompatibility = 11 // or higher
        targetCompatibility = 11 // or higher
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
            Runtime: java11
    
            Tracing: Active
            Environment:
                Variables:
                    POWERTOOLS_SERVICE_NAME: example
    ```

The Powertools for AWS Lambda (Java) service name is used as the X-Ray namespace. This can be set using the environment variable
`POWERTOOLS_SERVICE_NAME`

### Lambda handler

You can enable tracing using either the `@Tracing` annotation or the functional API.

**With the `@Tracing` annotation**, add it to your `handleRequest` method or any method to capture it as a separate subsegment automatically. You can optionally customize the segment name that appears in traces.

**With the functional API**, use `TracingUtils.withSubsegment()` to manually create subsegments without AspectJ configuration.

=== "@Tracing annotation"

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

=== "Functional API"

    ```java hl_lines="1 6 7 8 10 11 12"
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.withSubsegment("businessLogic1", subsegment -> {
                // Business logic 1
            });
    
            TracingUtils.withSubsegment("businessLogic2", subsegment -> {
                // Business logic 2
            });
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

When using the `@Tracing` annotation, the utility performs these additional tasks to ease operations:

  * Creates a `ColdStart` annotation to easily filter traces that have had an initialization overhead.
  * Creates a `Service` annotation if service parameter or `POWERTOOLS_SERVICE_NAME` is set.
  * Captures any response, or full exceptions generated by the handler, and include as tracing metadata.

By default, the `@Tracing` annotation uses `captureMode=ENVIRONMENT_VAR`, which means it will only record method responses and exceptions if you set
the environment variables `POWERTOOLS_TRACER_CAPTURE_RESPONSE` and `POWERTOOLS_TRACER_CAPTURE_ERROR` to `true`. You can override this behavior by
specifying a different `captureMode` to always record response, exception, both, or neither.

!!! note
    When using the functional API with `TracingUtils.withSubsegment()`, response and exception capture is not automatic. You can manually add metadata using `TracingUtils.putMetadata()` as needed.

!!! warning "Returning sensitive information from your Lambda handler or functions?"
    When using the `@Tracing` annotation, you can disable it from capturing responses and exceptions as tracing metadata with **`captureMode=DISABLED`**
    or globally by setting environment variables **`POWERTOOLS_TRACER_CAPTURE_RESPONSE`** and **`POWERTOOLS_TRACER_CAPTURE_ERROR`** to **`false`**.
    When using the functional API, you have full control over what metadata is captured.

=== "@Tracing annotation - Disable on method"

    ```java hl_lines="3"
    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        @Tracing(captureMode=CaptureMode.DISABLED)
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ...
        }
    ```

=== "@Tracing annotation - Disable Globally"

    ```yaml hl_lines="11 12"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java11
    
            Tracing: Active
            Environment:
                Variables:
                    POWERTOOLS_TRACER_CAPTURE_RESPONSE: false
                    POWERTOOLS_TRACER_CAPTURE_ERROR: false
    ```

=== "Functional API"

    ```java hl_lines="6 7 8"
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.withSubsegment("businessLogic", subsegment -> {
                // With functional API, you control what metadata is captured
            });
        }
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

## Advanced usage

### Multi-threaded programming

When working with multiple threads, you need to pass the trace entity to ensure proper trace context propagation.

=== "Multi-threaded example"

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

### AWS SDK for Java 2.x

Powertools for AWS Lambda (Java) includes the `aws-xray-recorder-sdk-aws-sdk-v2-instrumentor` library, which **automatically instruments all AWS SDK v2 clients** when you add the `powertools-tracing` dependency to your project. This means downstream calls to AWS services are traced without any additional configuration.

If you need more control over which clients are instrumented, you can manually add the `TracingInterceptor` to specific clients:

=== "Manual instrumentation (optional)"

    ```java hl_lines="1 2 3 8 9 10 11"
    import com.amazonaws.xray.interceptors.TracingInterceptor;
    import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
    import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

    public class LambdaHandler {
        private DynamoDbClient client = DynamoDbClient.builder()
            .region(Region.US_WEST_2)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .addExecutionInterceptor(new TracingInterceptor())
                .build()
            )
            .build();
        // ...
    }
    ```

For more details, refer to the [AWS X-Ray documentation on tracing AWS SDK calls](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java-awssdkclients.html) and [outgoing HTTP calls](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java-httpclients.html).

## Testing your code

When using `@Tracing` annotation, your Junit test cases needs to be configured to create parent Segment required by [AWS X-Ray SDK for Java](https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java.html).

Below are two ways in which you can configure your tests.

#### Configure environment variable on project level (Recommended)

You can choose to configure environment variable on project level for your test cases run. This is recommended approach as it will avoid the need of configuring each test case specifically.

Below are examples configuring your maven/gradle projects. You can choose to configure it differently as well as long as you are making sure that environment variable `LAMBDA_TASK_ROOT` is set. This variable is 
used internally via AWS X-Ray SDK to configure itself properly for lambda runtime.

=== "Maven (pom.xml)"

    ```xml
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

=== "Gradle (build.gradle)"
    
    ```json
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
