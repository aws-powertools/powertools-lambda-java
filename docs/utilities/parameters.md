---
title: Parameters
description: Utility
---


The parameters utilities provide a way to retrieve parameter values from
[AWS Systems Manager Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html), 
[AWS Secrets Manager](https://aws.amazon.com/secrets-manager/), or [Amazon DynamoDB](https://aws.amazon.com/dynamodb/). 
 
**Key features**

* Retrieve one or multiple parameters from an underlying provider in a standard way
* Cache parameter values for a given amount of time (defaults to 5 seconds)
* Transform parameter values from JSON or base 64 encoded strings

## Install
In order to provide lightweight dependencies, each parameters module is available as its own
package:

* **Secrets Manager** - `powertools-parameters-secrets` - [A service that centrally manages the lifecycle of secrets](https://aws.amazon.com/secrets-manager/). Start here if you need to manage secrets only. 
* **SSM Parameter Store** - `powertools-parameters-ssm` - [A secure, hierarchical store for configuration data management](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html). Start here if you need to manage various types of configuration data. 
* **Amazon DynamoDB** -`powertools-parameters-dynamodb` - [A fast, flexible, NoSQL database](https://aws.amazon.com/dynamodb/)
* **AWS AppConfig** - `powertools-parameters-appconfig` - [An application focussed configuration store](https://aws.amazon.com/systems-manager/features/appconfig) - start here if you are interested in advanced use cases, such as incremental deployment of configuration changes

You can easily mix and match parameter providers within the same project for different needs.  

Depending on which Java version you are using, you configuration will differ. Note that you must also provide
the concrete parameters module you want to use below - see the TODOs!

=== "Maven Java 11+"

    ```xml hl_lines="4-7 16 18 26-28"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
             <!-- TODO! Provide the parameters module you want to use here -->
             <artifactId>powertools-parameters-secrets</artifactId>
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
                             <!-- TODO! Provide the parameters module you want to use here -->
                             <artifactId>powertools-parameters-secrets</artifactId>
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

    ```xml hl_lines="4-7 16 18 26-28"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
             <!-- TODO! Provide the parameters module you want to use here -->
             <artifactId>powertools-parameters-secrets</artifactId>
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
                             <!-- TODO! Provide the parameters module you want to use here -->
                             <artifactId>powertools-parameters</artifactId>
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

    ```groovy hl_lines="3 11 12"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '8.1.0'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            // TODO! Provide the parameters module you want to use here
            aspect 'software.amazon.lambda:powertools-parameters-secrets:{{ powertools.version }}'
        }
        
        sourceCompatibility = 11 // or higher
        targetCompatibility = 11 // or higher
    ```

=== "Gradle Java 1.8"

    ```groovy hl_lines="3 11 12"
        plugins {
            id 'java'
            id 'io.freefair.aspectj.post-compile-weaving' version '6.6.3'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            // TODO! Provide the parameters module you want to use here
            aspect 'software.amazon.lambda:powertools-parameters-secrets:{{ powertools.version }}'
        }
        
        sourceCompatibility = 1.8
        targetCompatibility = 1.8
    ```

**IAM Permissions**

This utility requires additional permissions to work as expected. See the table below:

Provider | Function/Method                                                      | IAM Permission
------------------------------------------------- |----------------------------------------------------------------------| ---------------------------------------------------------------------------------
SSM Parameter Store | `SSMProvider.get(String)` `SSMProvider.get(String, Class)`           | `ssm:GetParameter`
SSM Parameter Store | `SSMProvider.getMultiple(String)`                                    | `ssm:GetParametersByPath`
Secrets Manager | `SecretsProvider.get(String)` `SecretsProvider.get(String, Class)`   | `secretsmanager:GetSecretValue`
DynamoDB | `DynamoDBProvider.get(String)` `DynamoDBProvider.getMultiple(string)` | `dynamodb:GetItem` `dynamoDB:Query`

## Retrieving Parameters
You can retrieve parameters either using annotations or by using the `xParamProvider` class for each parameter
provider directly. The latter is useful if you need to configure the underlying SDK client, for example to use
a different region or credentials, the former is simpler to use.

### Using Annotations

=== "Secrets Manager: @SecretsParam"

    ```java hl_lines="9 10"
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.parameters.secrets.SecretsParam;
    
    public class ParametersFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Annotation-style injection from secrets manager
    @SecretsParam(key = "/powertools-java/userpwd")
    String secretParam;
    ```

=== "Systems Manager: @SSMParam"

    ```java hl_lines="9 10"
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.parameters.ssm.SSMParam;

    public class ParametersFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Annotation-style injection from SSM Parameter Store
    @SSMParam(key = "/powertools-java/param")
    String ssmParam;
    ```

=== "DynamoDB: @DyanmoDbParam"

    ```java hl_lines="9 10"
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.parameters.dynamodb.DynamoDBParam;

    public class ParametersFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Annotation-style injection from DynamoDB
    @DynamoDbParam(table = "my-test-tablename", key = "myKey")
    String ddbParam;
    ```

=== "AppConfig: @AppConfigParam"

    ```java hl_lines="9 10"
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.parameters.appconfig.AppConfigParam;

    public class ParametersFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    // Annotation-style injection from AppConfig
    @AppConfigParam(application = "my-app", environment = "my-env", key = "myKey")
    String appConfigParam;
    ```


### Using the `ParamProvider` classes

=== "Secrets Manager"

    ```java hl_lines="15-19 23-28"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
    import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
    
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import java.time.temporal.ChronoUnit;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        // Get an instance of the SecretsProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        SecretsProvider secretsProvider = SecretsProvider
                .builder()
                .withClient(SecretsManagerClient.builder().build())
                .build();
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Retrieve a single secret
            String value = secretsProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
    
            // Return the result
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(value);
        }
    }
    ```

=== "Systems Manager"

    ```java hl_lines="15-19 23-28"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.awssdk.services.ssm.SsmClient;
    import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;
    import java.time.temporal.ChronoUnit;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        // Get an instance of the SSMProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        SSMProvider ssmProvider = SSMProvider
                .builder()
                .withClient(SsmClient.builder().build())
                .build();
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Retrieve a single secret
            String value = ssmProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
    
            // Return the result
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(value);
        }
    }
    ```

=== "DynamoDB"

    ```java hl_lines="15-19 23-28"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
    import software.amazon.lambda.powertools.parameters.dynamodb.DynamoDbProvider;
    import java.time.temporal.ChronoUnit;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        // Get an instance of the SecretsProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        DynamoDbProvider ddbProvider = DynamoDbProvider
                .builder()
                .withClient(DynamoDbClient.builder().build())
                .build();
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Retrieve a single secret
            String value = ddbProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
    
            // Return the result
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(value);
        }
    }
    ```

=== "AppConfig"

    ```java hl_lines="15-19 23-28"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
    import software.amazon.lambda.powertools.parameters.appconfig.AppConfigProvider;
    import java.time.temporal.ChronoUnit;

    public class AppWithSecrets implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
        // Get an instance of the SecretsProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        AppConfigProvider appConfigProvider = AppConfigProvider
                .builder()
                .withClient(AppConfigDataClient.builder().build())
                .build();
    
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            // Retrieve a single secret
            String value = appConfigProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
    
            // Return the result
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(value);
        }
    }    
    ```

## Advanced configuration


### Transform values

Parameter values can be transformed using ```withTransformation(transformerClass)```.
Base64 and JSON transformations are provided. For more complex transformation, you need to specify how to deserialize.

!!! warning "`SSMProvider.getMultiple()` does not support transformation and will return simple Strings."

=== "Base64 Transformation"
    ```java
       String value = provider
                        .withTransformation(Transformer.base64)
                        .get("/my/parameter/b64");
    ```

=== "Complex Transformation"

    ```java
       MyObj object = provider
                        .withTransformation(Transformer.json)
                        .get("/my/parameter/json", MyObj.class);
    ```

### Write your own Transformer

You can write your own transformer, by implementing the `Transformer` interface and the `applyTransformation()` method.
For example, if you wish to deserialize XML into an object.

=== "XmlTransformer.java"

    ```java hl_lines="1"
    public class XmlTransformer<T> implements Transformer<T> {
    
        private final XmlMapper mapper = new XmlMapper();
    
        @Override
        public T applyTransformation(String value, Class<T> targetClass) throws TransformationException {
            try {
                return mapper.readValue(value, targetClass);
            } catch (IOException e) {
                throw new TransformationException(e);
            }
        }
    }
    ```

=== "Using XmlTransformer"

    ```java
        MyObj object = provider
                            .withTransformation(XmlTransformer.class)
                            .get("/my/parameter/xml", MyObj.class);
    ```
