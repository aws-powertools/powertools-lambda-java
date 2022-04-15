---
title: Cors
description: Utility
---

The Cors utility helps configuring [CORS (Cross-Origin Resource Sharing)](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) for your Lambda function when used with API Gateway and Lambda proxy integration.
When configured as Lambda proxy integration, the function must set CORS HTTP headers in the response ([doc](https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-cors.html#:~:text=Enabling%20CORS%20support%20for%20Lambda%20or%20HTTP%20proxy%20integrations)).

**Key features**

* Automatically set the CORS HTTP headers in the response.
* Multi-origins is supported, only the origin matching the request origin will be returned.
* Support environment variables or programmatic configuration

## Install

To install this utility, add the following dependency to your project.

=== "Maven"

    ```xml
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-cors</artifactId>
        <version>{{ powertools.version }}</version>
    </dependency>

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
                             <artifactId>powertools-cors</artifactId>
                         </aspectLibrary>
                         ...
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
=== "Gradle"

    ```groovy
     plugins{
        id 'java'
        id 'io.freefair.aspectj.post-compile-weaving' version '6.3.0'
     }

     dependencies {
        ...
        aspect 'software.amazon.lambda:powertools-cors:{{ powertools.version }}'
     }
    ```

## Cors Annotation

You can use the `@CrossOrigin` annotation on the `handleRequest` method of a class that implements `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`

=== "Cross Origin Annotation"

```java hl_lines="3"
public class FunctionProxy implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @CrossOrigin
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // ...
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(headers)
            .withBody(body);
    }
}
```

## Configuration

### Using the annotation

=== "FunctionProxy.java"

The annotation provides all the parameters required to set up the different CORS headers:

| parameter            | Default                                                                                                                     | Description                                                                                                                             |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **allowedHeaders**   | `Authorization, *` ([*](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers#directives)) | [`Access-Control-Allow-Headers`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers) header         |
| **exposedHeaders**   | `*`                                                                                                                         | [`Access-Control-Expose-Headers`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers) header       |
| **origins**          | `*`                                                                                                                         | [`Access-Control-Allow-Origin`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin) header           |
| **methods**          | `*`                                                                                                                         | [`Access-Control-Allow-Methods`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Methods) header         | 
| **allowCredentials** | `true`                                                                                                                      | [`Access-Control-Allow-Credentials`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials) header |
| **maxAge**           | `29`                                                                                                                        | [`Access-Control-Max-Age`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age) header                     |

**Example:**

=== "FunctionProxy.java"

```java hl_lines="3-7"
public class FunctionProxy implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @CrossOrigin(
        origins = "http://origin.com, https://other.origin.com",
        allowedHeaders = "Authorization, Content-Type, X-API-Key",
        methods = "POST, OPTIONS"
    )
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // ...
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(headers)
            .withBody(body);
    }
}
```


### Using Environment variables

You can configure the CORS header values in the environment variables of your Lambda function. They will have precedence over the annotation configuration.
This way you can externalize the configuration and change it without changing the code.

| Environment variable                 | Default                                                                                                                     | Description                                                                                                                             |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **ACCESS_CONTROL_ALLOW_HEADERS**     | `Authorization, *` ([*](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers#directives)) | [`Access-Control-Allow-Headers`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers) header         |
| **ACCESS_CONTROL_EXPOSE_HEADERS**    | `*`                                                                                                                         | [`Access-Control-Expose-Headers`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers) header       |
| **ACCESS_CONTROL_ALLOW_ORIGIN**      | `*`                                                                                                                         | [`Access-Control-Allow-Origin`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin) header           |
| **ACCESS_CONTROL_ALLOW_METHODS**     | `*`                                                                                                                         | [`Access-Control-Allow-Methods`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Methods) header         | 
| **ACCESS_CONTROL_ALLOW_CREDENTIALS** | `true`                                                                                                                      | [`Access-Control-Allow-Credentials`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials) header |
| **ACCESS_CONTROL_MAX_AGE**           | `29`                                                                                                                        | [`Access-Control-Max-Age`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age) header                     |

**Example:**

=== "SAM template"

```yaml hl_lines="12-17"
  CorsFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: Function
      Handler: CorsFunction::handleRequest
      Runtime: java11
      Architectures:
        - x86_64
      MemorySize: 512
      Environment:
        Variables:
          ACCESS_CONTROL_ALLOW_HEADERS: 'Authorization, Content-Type, X-API-Key'
          ACCESS_CONTROL_EXPOSE_HEADERS: '*'
          ACCESS_CONTROL_ALLOW_ORIGIN: 'https://mydomain.com'
          ACCESS_CONTROL_ALLOW_METHODS: 'OPTIONS, POST'
          ACCESS_CONTROL_MAX_AGE: '300'
          ACCESS_CONTROL_ALLOW_CREDENTIALS: 'true'
      Events:
        MyApi:
          Type: Api
          Properties:
            Path: /cors
            Method: post
```

## Advanced information about origin configuration
Browsers do no support multiple origins (comma-separated) in the `Access-Control-Allow-Origin` header. Only one must be provided.
If your backend can be reached by multiple frontend applications (with different origins), 
the function must return the `Access-Control-Allow-Origin` header that matches the `origin` header in the request.

Lambda Powertools handles this for you. You can configure multiple origins, separated by commas:

=== "Multiple origins"

```java hl_lines="2"
  @CrossOrigin(
    origins = "http://origin.com, https://other.origin.com"
  )
```

!!! warning "Origins must be well-formed"
    Origins must be well-formed URLs: `{protocol}://{host}[:{port}]` where protocol can be `http` or `https` and port is optional. [Mode details](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy#definition_of_an_origin).
    
    `*`, `http://*` and `https://*` are also valid origins.

!!! info "`Vary` header"
    Note that when returning a specific value (rather than `*`), the `Vary` header must be set to `Origin` ([doc](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin#cors_and_caching)). Lambda Powertools handles this for you.