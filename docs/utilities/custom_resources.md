---
title: Custom Resources 
description: Utility
---

[CloudFormation Custom resources](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources.html)
provide a way for [AWS Lambda functions](
https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources-lambda.html) to execute
provisioning logic whenever CloudFormation stacks are created, updated, or deleted. 

Powertools-cloudformation makes it easy to write Lambda functions in Java that are used as CloudFormation custom resources.    
The utility reads incoming CloudFormation events, calls your custom code depending on the operation (CREATE, UPDATE or DELETE) and sends responses back to CloudFormation.  
By using this library you do not need to write code to integrate with CloudFormation, and you only focus on writing the custom provisioning logic inside the Lambda function.

## Install

To install this utility, add the following dependency to your project.

=== "Maven"

    ```xml
    <dependency>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-cloudformation</artifactId>
        <version>{{ powertools.version }}</version>
    </dependency>
    ```

=== "Gradle"

    ```groovy
     dependencies {
        ...
        implementation 'software.amazon.lambda:powertools-cloudformation:{{ powertools.version }}'
    }
    ```

## Usage

To utilise the feature, extend the `AbstractCustomResourceHandler` class in your Lambda handler class.  
After you extend the `AbstractCustomResourceHandler`, implement and override the following 3 methods: `create`, `update` and `delete`. The `AbstractCustomResourceHandler` invokes the right method according to the CloudFormation [custom resource request event](
https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/crpg-ref-requests.html) it receives.  
Inside the methods, implement your custom provisioning logic, and return a `Response`. The `AbstractCustomResourceHandler` takes your `Response`, builds a
[custom resource responses](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/crpg-ref-responses.html) and sends it to CloudFormation automatically.  

Custom resources notify cloudformation either of `SUCCESS` or `FAILED` status. You have 2 utility methods to represent these responses: `Response.success(physicalResourceId)` and `Response.failed(physicalResourceId)`.
If a `Response` is not returned by your code, `AbstractCustomResourceHandler` defaults the response to `SUCCESS`.

```java hl_lines="8 9 10 11"
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

public class ProvisionOnCreateHandler extends AbstractCustomResourceHandler {

    @Override
    protected Response create(CloudFormationCustomResourceEvent createEvent, Context context) {
        String physicalResourceId = "sample-resource-id-" + UUID.randomUUID(); //Create a unique ID for your resource
        ProvisioningResult provisioningResult = doProvisioning();
        if(provisioningResult.isSuccessful()){ //check if the provisioning was successful
            return Response.success(physicalResourceId);
        }else{
            return Response.failed(physicalResourceId);
        }
    }

    @Override
    protected Response update(CloudFormationCustomResourceEvent updateEvent, Context context) {
        return null;
    }

    @Override
    protected Response delete(CloudFormationCustomResourceEvent deleteEvent, Context context) {
        return null;
    }
}
```

### Signaling Provisioning Failures

If the provisioning inside your Custom Resource fails, you can notify CloudFormation of the failure by returning a `Repsonse.failure(physicalResourceId)`.


### Configuring Response Objects

When provisioning results in data to be shared with other parts of the stack, include this data within the returned
`Response` instance.

This Lambda function creates a [Chime AppInstance](https://docs.aws.amazon.com/chime/latest/dg/create-app-instance.html)
and maps the returned ARN to a "ChimeAppInstanceArn" attribute.

```java hl_lines="11 12 13 14"
public class ChimeAppInstanceHandler extends AbstractCustomResourceHandler {
    @Override
    protected Response create(CloudFormationCustomResourceEvent createEvent, Context context) {
        CreateAppInstanceRequest chimeRequest = CreateAppInstanceRequest.builder()
                .name("my-app-name")
                .build();
        CreateAppInstanceResponse chimeResponse = ChimeClient.builder()
                .region("us-east-1")
                .createAppInstance(chimeRequest);

        Map<String, String> chimeAtts = Map.of("ChimeAppInstanceArn", chimeResponse.appInstanceArn());
        return Response.builder()
                .value(chimeAtts)
                .build();
    }
}
```

For the example above the following response payload will be sent.

```json
{
  "Status": "SUCCESS",
  "PhysicalResourceId": "2021/10/01/e3a37e552eff4718a5675c1e31f0649e",
  "StackId": "arn:aws:cloudformation:us-east-1:123456789000:stack/Custom-stack/59e4d2d0-2fe2-10ec-b00e-124d7c1c5f15",
  "RequestId": "7cae0346-0359-4dff-b80a-a82f247467b6",
  "LogicalResourceId:": "ChimeTriggerResource",
  "NoEcho": false,
  "Data": {
    "ChimeAppInstanceArn": "arn:aws:chime:us-east-1:123456789000:app-instance/150972c2-5490-49a9-8ba7-e7da4257c16a"
  }
}
```

Once the custom resource receives this response, it's "ChimeAppInstanceArn" attribute is set and the
[Fn::GetAtt function](
https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html) may be used to
retrieve the attribute value and make it available to other resources in the stack.

#### Sensitive Response Data

If any attributes are sensitive, enable the "noEcho" flag to mask the output of the custom resource when it's retrieved
with the Fn::GetAtt function.

```java hl_lines="6"
public class SensitiveDataHandler extends AbstractResourceHandler {
    @Override
    protected Response create(CloudFormationCustomResourceEvent createEvent, Context context) {
        return Response.builder()
                .value(Map.of("SomeSecret", sensitiveValue))
                .noEcho(true)
                .build();
    }
}
```

#### Customizing Serialization

Although using a `Map` as the Response's value is the most straightforward way to provide attribute name/value pairs,
any arbitrary `java.lang.Object` may be used. By default, these objects are serialized with an internal Jackson
`ObjectMapper`. If the object requires special serialization logic, a custom `ObjectMapper` can be specified.

```java hl_lines="21 22 23 24"
public class CustomSerializationHandler extends AbstractResourceHandler {
    /**
     * Type representing the custom response Data. 
     */
    static class Policy {
        public ZonedDateTime getExpires() {
            return ZonedDateTime.now().plusDays(10);
        }
    }

    /**
     * Mapper for serializing Policy instances.
     */
    private final ObjectMapper policyMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    protected Response create(CloudFormationCustomResourceEvent createEvent, Context context) {
        Policy policy = new Policy();
        return Response.builder()
                .value(policy)
                .objectMapper(policyMapper) // customize serialization
                .build();
    }
}
```
