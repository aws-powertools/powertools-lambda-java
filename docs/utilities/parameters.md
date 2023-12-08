---
title: Parameters
description: Utility
---


The parameters utilities provide a way to retrieve parameter values from
[AWS Systems Manager Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html), 
[AWS Secrets Manager](https://aws.amazon.com/secrets-manager/), [Amazon DynamoDB](https://aws.amazon.com/dynamodb/), 
or [AWS AppConfig](https://aws.amazon.com/systems-manager/features/appconfig/). 
 
## Key features

* Retrieve one or multiple parameters from an underlying provider in a standard way
* Cache parameter values for a given amount of time (defaults to 5 seconds)
* Transform parameter values from JSON or base 64 encoded strings

## Install
In order to provide lightweight dependencies, each parameters module is available as its own
package:

* **Secrets Manager** - `powertools-parameters-secrets`  
* **SSM Parameter Store** - `powertools-parameters-ssm`  
* **Amazon DynamoDB** -`powertools-parameters-dynamodb` 
* **AWS AppConfig** - `powertools-parameters-appconfig`

You can easily mix and match parameter providers within the same project for different needs.  

Depending on which Java version you are using, you configuration will differ. Note that you must also provide
the concrete parameters module you want to use below - see the TODOs!

=== "Maven Java 11+"

    ```xml hl_lines="4-12 17 24 30-34"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>

             <!-- TODO! Provide the parameters module you want to use here -->
             <artifactId>powertools-parameters-secrets</artifactId>
             <artifactId>powertools-parameters-ssm</artifactId>
             <artifactId>powertools-parameters-dynamodb</artifactId>
             <artifactId>powertools-parameters-appconfig</artifactId>

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
                         <!-- TODO! Provide an aspectLibrary for each of the parameters module(s) you want to use here -->
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
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

    ```xml hl_lines="4-12 17 24 30-34"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>

             <!-- TODO! Provide the parameters module you want to use here -->
             <artifactId>powertools-parameters-secrets</artifactId>
             <artifactId>powertools-parameters-ssm</artifactId>
             <artifactId>powertools-parameters-dynamodb</artifactId>
             <artifactId>powertools-parameters-appconfig</artifactId>

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
                         <!-- TODO! Provide an aspectLibrary for each of the parameters module(s) you want to use here -->
                         <aspectLibrary>
                             <groupId>software.amazon.lambda</groupId>
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
        
         // TODO! Provide an aspectLibrary for each of the parameters module(s) you want to use here
        dependencies {
            aspect 'software.amazon.lambda:powertools-parameters-secrets:{{ powertools.version }}'
        }
        
        sourceCompatibility = 1.8
        targetCompatibility = 1.8
    ```

**IAM Permissions**

This utility requires additional permissions to work as expected. See the table below:

| Provider  | Function/Method                                                         | IAM Permission                                                            |
|-----------|-------------------------------------------------------------------------|---------------------------------------------------------------------------|
| SSM       | `SSMProvider.get(String)` `SSMProvider.get(String, Class)`              | `ssm:GetParameter`                                                        |
| SSM       | `SSMProvider.getMultiple(String)`                                       | `ssm:GetParametersByPath`                                                 |
| SSM       | If using `withDecryption(true)`                                         | You must add an additional permission `kms:Decrypt`                       |
| Secrets   | `SecretsProvider.get(String)` `SecretsProvider.get(String, Class)`      | `secretsmanager:GetSecretValue`                                           |
| DynamoDB  | `DynamoDBProvider.get(String)` `DynamoDBProvider.getMultiple(string)`   | `dynamodb:GetItem` `dynamoDB:Query`                                       |
| AppConfig | `AppConfigProvider.get(String)` `AppConfigProvider.getMultiple(string)` | `appconfig:StartConfigurationSession`, `appConfig:GetLatestConfiguration` |

## Retrieving Parameters
You can retrieve parameters either using annotations or by using the `xParamProvider` class for each parameter
provider directly. The latter is useful if you need to configure the underlying SDK client, for example to use
a different region or credentials, the former is simpler to use.

### Using Annotations

=== "Secrets Manager: @SecretsParam"

    ```java hl_lines="8 9"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.lambda.powertools.parameters.secrets.SecretsParam;
    
    public class ParametersFunction implements RequestHandler<String, String> {

        // Annotation-style injection from secrets manager
        @SecretsParam(key = "/powertools-java/userpwd")
        String secretParam;

        public string handleRequest(String request, Context context) {
            // Because this is a secret, we probably don't want to return it! Return something indicating
            // we could access it instead.
            return "Retrieved a secret, and sensibly refusing to return it!";
        }
    }
    ```

=== "SSM Parameters Store: @SSMParam"

    ```java hl_lines="8 9"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.lambda.powertools.parameters.ssm.SSMParam;

    public class ParametersFunction implements RequestHandler<String, String> {

        // Annotation-style injection from SSM Parameter Store
        @SSMParam(key = "/powertools-java/param")
        String ssmParam;

        public string handleRequest(String request, Context context) {
            return ssmParam; // Request handler simply returns our configuration value
        }
    }
    ```

=== "DynamoDB: @DyanmoDbParam"

    ```java hl_lines="8 9"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.lambda.powertools.parameters.dynamodb.DynamoDBParam;

    public class ParametersFunction implements RequestHandler<String, String> {

        // Annotation-style injection from DynamoDB
        @DynamoDbParam(table = "my-test-tablename", key = "myKey")
        String ddbParam;

        public string handleRequest(String request, Context context) {
            return ddbParam;  // Request handler simply returns our configuration value
        }
    }
    ```

=== "AppConfig: @AppConfigParam"

    ```java hl_lines="8 9"
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.lambda.powertools.parameters.appconfig.AppConfigParam;

    public class ParametersFunction implements RequestHandler<String, String> {
    
        // Annotation-style injection from AppConfig
        @AppConfigParam(application = "my-app", environment = "my-env", key = "myKey")
        String appConfigParam;

        public string handleRequest(String request, Context context) {
            return appConfigParam; // Request handler simply returns our configuration value
        }
    }
    ```


### Using the `ParamProvider` classes

=== "Secrets Manager"

    ```java hl_lines="13-16 20-25"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
    import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import java.time.temporal.ChronoUnit;

    public class RequestHandlerWithParams implements RequestHandler<String, String> {
    
        // Get an instance of the SecretsProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        SecretsProvider secretsProvider = SecretsProvider
                .builder()
                .withClient(SecretsManagerClient.builder().build())
                .build();
    
        public String handleRequest(String input, Context context) {
            // Retrieve a single secret
            String value = secretsProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
    
            // Because this is a secret, we probably don't want to return it! Return something indicating
            // we could access it instead.
            return "Retrieved a secret, and sensibly refusing to return it!";
        }
    }
    ```

=== "Systems Manager"

    ```java hl_lines="13-16 20-27"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.awssdk.services.ssm.SsmClient;
    import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;
    import java.time.temporal.ChronoUnit;

    public class RequestHandlerWithParams implements RequestHandler<String, String> {
    
        // Get an instance of the SSMProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        SSMProvider ssmProvider = SSMProvider
                .builder()
                .withClient(SsmClient.builder().build())
                .build();
    
        public String handleRequest(String input, Context context) {
            // Retrieve a single secret
            String value = ssmProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
                    // We might instead want to retrieve multiple secrets at once, returning a Map of key/value pairs
                    // .getMultiple("/my/secret/path");

            // Return the result
            return value;
        }
    }
    ```

=== "DynamoDB"

    ```java hl_lines="13-16 20-27"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
    import software.amazon.lambda.powertools.parameters.dynamodb.DynamoDbProvider;
    import java.time.temporal.ChronoUnit;

    public class RequestHandlerWithParams implements RequestHandler<String, String> {
    
        // Get an instance of the SecretsProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        DynamoDbProvider ddbProvider = DynamoDbProvider
                .builder()
                .withClient(DynamoDbClient.builder().build())
                .build();
    
        public String handleRequest(String input, Context context) {
            // Retrieve a single secret
            String value = ddbProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
                    // We might instead want to retrieve multiple values at once, returning a Map of key/value pairs
                    // .getMultiple("my-partition-key-value");
    
            // Return the result
            return value;
        }
    }
    ```

=== "AppConfig"

    ```java hl_lines="13-16 20-25"
    import static software.amazon.lambda.powertools.parameters.transform.Transformer.base64;
    
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
    import software.amazon.lambda.powertools.parameters.appconfig.AppConfigProvider;
    import java.time.temporal.ChronoUnit;

    public class RequestHandlerWithParams implements RequestHandler<String, String> {
    
        // Get an instance of the SecretsProvider. We can provide a custom client here if we want,
        // for instance to use a particular region.
        AppConfigProvider appConfigProvider = AppConfigProvider
                .builder()
                .withClient(AppConfigDataClient.builder().build())
                .build();
    
        public String handleRequest(String input, Context context) {
            // Retrieve a single secret
            String value = appConfigProvider
                    // Transform parameter from base64
                    .withTransformation(base64)
                    // By default values are cached for 5 seconds, specify 10 seconds instead.
                    .withMaxAge(10, ChronoUnit.SECONDS)
                    .get("/my/secret");
    
            // Return the result
            return value;
        }
    }    
    ```

## Advanced configuration

### Default cache timeout
Each provider uses the CacheManager to cache parameter values. By default, values are cached for 5 seconds. If you
wish to customize this, you can provide a custom `CacheManager` to your provider with an appropriate timeout configured.
`CacheManager` instances can also be shared between providers.

=== "Customize Cache"

    ```java hl_lines="9 10 14 19"
    import java.time.Duration;
    import software.amazon.lambda.powertools.parameters.appconfig.AppConfigProvider;
    import software.amazon.lambda.powertools.parameters.cache.CacheManager;
    
    public class CustomizeCache {
        
        public void CustomizeCache() {
    
            CacheManager cacheManager = new CacheManager();
            cacheManager.setDefaultExpirationTime(Duration.ofSeconds(10));
    
            AppConfigProvider paramProvider = AppConfigProvider
                    .builder()
                    .withCacheManager(cacheManager)
                    .withClient(AppConfigDataClient.builder().build())
                    .build();
    
            // We can override the default per `get` 
            paramProvider.withMaxAge(20, ChronoUnit.SECONDS)
                    .get("my-param");
        }
    }
    ```


### Transform values

Parameter values can be transformed using ```withTransformation(transformerClass)```.
Base64 and JSON transformations are provided. For more complex transformation, you need to specify how to deserialize.

!!! warning "`getMultiple()` does not support transformation and will return simple Strings."

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

### Create your own Transformer

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

### Create your own Provider
You can create your own custom parameter store provider by implementing a handful of classes: 

=== "CustomProvider.java"

    ```java
    import java.util.Map;
    import software.amazon.lambda.powertools.parameters.BaseProvider;
    import software.amazon.lambda.powertools.parameters.cache.CacheManager;
    import software.amazon.lambda.powertools.parameters.transform.TransformationManager;
    
    public class CustomProvider extends BaseProvider {
    
        public CustomProvider(CacheManager cacheManager, TransformationManager transformationManager) {
            super(cacheManager, transformationManager);
        }

        public CustomProviderBuilder builder() {
            return new CustomProviderBuilder();
        }
    
        @Override
        protected String getValue(String key) {
            throw new RuntimeException("TODO - return a single value");
        }
    
        @Override
        protected Map<String, String> getMultipleValues(String path) {
            throw new RuntimeException("TODO - Optional - return multiple values");
        }
    }
    ```

=== "CustomProviderBuilder.java"

    ```java
    public class CustomProviderBuilder {
        private CacheManager cacheManager;
        private TransformationManager transformationManager;

        /**
         * Create a {@link DynamoDbProvider} instance.
         *
         * @return a {@link DynamoDbProvider}
         */
        public CustomProvider build() {
            if (cacheManager == null) {
                cacheManager = new CacheManager();
            }
            return new CustomProvider(cacheManager, transformationManager);
        }

        /**
         * Provide a CacheManager to the {@link CustomProvider}
         *
         * @param cacheManager the manager that will handle the cache of parameters
         * @return the builder to chain calls (eg. <pre>builder.withCacheManager().build()</pre>)
         */
        public CustomProviderBuilder withCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        /**
         * Provide a transformationManager to the {@link DynamoDbProvider}
         *
         * @param transformationManager the manager that will handle transformation of parameters
         * @return the builder to chain calls (eg. <pre>builder.withTransformationManager().build()</pre>)
         */
        public CustomProviderBuilder withTransformationManager(TransformationManager transformationManager) {
            this.transformationManager = transformationManager;
            return this;
        }
    }
    ```

=== "CustomProviderParam.java"
    
    ```java 
    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;
    import software.amazon.lambda.powertools.parameters.transform.Transformer;
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface CustomProviderParam {
        // The parameter key  
        String key();

        // The transformer to use
        Class<? extends Transformer> transformer() default Transformer.class;
    }
    ```

=== "CustomProviderAspect.java"

    ```java
    @Aspect
    public class CustomProviderAspect extends BaseParamAspect {
    
        @Pointcut("get(* *) && @annotation(ddbConfigParam)")
        public void getParam(CustomProviderParam customConfigParam) {
        }
    
        @Around("getParam(customConfigParam)")
        public Object injectParam(final ProceedingJoinPoint joinPoint, final CustomProviderParam customConfigParam) {
            System.out.println("GET IT");
    
            BaseProvider provider = CustomProvider.builder().build();

            return getAndTransform(customConfigParam.key(), ddbConfigParam.transformer(), provider,
                    (FieldSignature) joinPoint.getSignature());
        }
    
    }
    ```