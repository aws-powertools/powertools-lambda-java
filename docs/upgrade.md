---
title: Upgrade guide
description: Guide to update between major Powertools for AWS Lambda (Java) versions
---

## End of support v1

<!-- TODO: Add end of support banner here once developer preview started. -->

Given our commitment to all of our customers using Powertools for AWS Lambda (Java), we will keep [Maven Central](https://central.sonatype.com/search?q=powertools){target="\_blank"} `v1` releases and a `v1` documentation archive to prevent any disruption.

## Migrate to v2 from v1

!!! info "We strongly encourage you to migrate to `v2`. Refer to our [versioning policy](./processes/versioning.md) to learn more about our version support process."

We've made minimal breaking changes to make your transition to `v2` as smooth as possible.

### Quick summary

The following table shows a summary of the changes made in `v2` and whether code changes are necessary. Each change that requires a code change links to a section below explaining more details.

| Area                 | Change                                                                                                                                                                                   | Code change required |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------- |
| **Logging**          | The [logging module was re-designed](#redesigned-logging-utility) from scratch to support popular Java logging paradigms and libraries like `log4j2`, `logback`, and `slf4j`.            | Yes                  |
| **Metrics**          | [Changed public interface](#updated-metrics-utility-interface) to remove direct coupling with `aws-embedded-metrics-java`.                                                               | Yes                  |
| **Tracing**          | [Removed deprecated `captureResponse` and `captureError` options](#deprecated-capture-mode-related-tracing-annotation-parameters) on `@Tracing` annotation.                              | Yes                  |
| **Idempotency**      | The [`powertools-idempotency` module was split by provider](#idempotency-utility-split-into-sub-modules-by-provider) to improve modularity and reduce the deployment package size.       | Yes                  |
| **Idempotency**      | Updated `IdempotencyConfig` interface to support addition of response hooks.                                                                                                             | No                   |
| **Parameters**       | The [`powertools-parameters` module was split by provider](#parameters-utility-split-into-sub-modules-by-provider) to improve modularity and reduce the deployment package size.         | Yes                  |
| **Batch Processing** | [Removed deprecated `powertools-sqs` module](#removed-powertools-sqs-module-in-favor-of-powertools-batch) in favor of the more generic [Batch Processing](./utilities/batch.md) utility. | Yes                  |
| **Batch Processing** | Updated Batch Processing `BatchMessageHandler` interface to add support for parallel processing.                                                                                         | No                   |
| **Validation**       | The `@Validation` utility returns 4xx error codes instead of 5xx error codes when used with API Gateway now.                                                                             | No                   |
| **Validation**       | Validating batch event sources now adds failed events as partial batch failures and does not fail the whole batch anymore.                                                               | No                   |
| **Custom Resources** | [Removed deprecated `Response.failed()` and `Response.success()` methods](#custom-resources-updates-the-response-class).                                                                 | Yes                  |
| **Custom Resources** | Changed interface of `Response` class to add an optional `reason` field.                                                                                                                 | No                   |
| **Dependencies**     | Renamed `powertools-core` to `powertools-common`. This module should not be used as direct dependency and is listed here for completeness.                                               | No                   |
| **Dependencies**     | [Removed `org.aspectj.aspectjrt` as project dependency](#aspectj-runtime-not-included-by-default-anymore) in favor of consumers including the version they prefer.                       | Yes                  |
| **Language support** | Removed support for Java 8. The minimum required Java version is Java 11.                                                                                                                | N/A                  |

### First Steps

Before you start, we suggest making a copy of your current working project or create a new branch with `git`.

1. **Upgrade** Java to at least version 11. While version 11 is supported, we recommend using the [newest available LTS version](https://downloads.corretto.aws/#/downloads){target="\_blank"} of Java.
2. **Review** the following section to confirm if you need to make changes to your code.

## Redesigned Logging Utility

<!--
- Add new logging module: https://github.com/aws-powertools/powertools-lambda-java/pull/1539
- Add advanced features to new logging module: https://github.com/aws-powertools/powertools-lambda-java/pull/1435
- Block reserved keys: https://github.com/aws-powertools/powertools-lambda-java/commit/374b38db3b91d421a14bb71b4df0194c72304efa
-->

The logging utility was re-designed from scratch to integrate better with Java idiomatic conventions and to remove the hard dependency on `log4j` as logging implementation. The new logging utility now supports `slfj4` as logging interface and gives you the choice among `log4j2` and `logback` as logging implementations. Consider the following steps to migrate from the v1 logging utility to the v2 logging utility:

**1. Remove `powertools-logging` dependency and replace it with your logging backend of choice**

In order to support different logging implementations, dedicated logging modules were created for the different logging implementations. Remove `powertools-logging` as a dependency and replace it with either `powertools-logging-log4j` or `powertools-logging-logback`.

```diff
<!-- BEFORE v2 -->
- <dependency>
-     <groupId>software.amazon.lambda</groupId>
-     <artifactId>powertools-logging</artifactId>
-     <version>1.x.x</version>
- </dependency>

<!-- AFTER v2 -->
+ <dependency>
+     <groupId>software.amazon.lambda</groupId>
+     <artifactId>powertools-logging-log4j</artifactId>
+     <version>2.x.x</version>
+ </dependency>
```

<!-- prettier-ignore-start -->
!!! info "The AspectJ configuration still needs to depend on `powertools-logging`"
    We have only replaced the logging implementation dependency. The AspectJ configuration still needs to depend on `powertools-logging` which contains the main logic.

    ```xml
    <aspectLibrary>
        <groupId>software.amazon.lambda</groupId>
        <artifactId>powertools-logging</artifactId>
    </aspectLibrary>
    ```
<!-- prettier-ignore-end -->

**2. Update `log4j2.xml` including new `JsonTemplateLayout`**

This step is only required if you are using log4j2 as your logging implementation. The deprecated `#!xml <LambdaJsonLayout/>` element was removed. Replace it with the log4j2 agnostic `#!xml <JsonTemplateLayout/>` element.

```diff
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="JsonAppender" target="SYSTEM_OUT">
-           <LambdaJsonLayout compact="true" eventEol="true"/>
+           <JsonTemplateLayout eventTemplateUri="classpath:LambdaJsonLayout.json" />
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

**3. Migrate all logging specific calls to SLF4J native primitives (recommended)**

The new logging utility is designed to integrate seamlessly with Java SLF4J to allow customers adopt the Logging utility without large code refactorings. This improvement requires the migration of non-native SLF4J primitives from the v1 Logging utility.

!!! info "While we recommend using SLF4J as a logging implementation independent facade, you can still use the log4j2 and logback interfaces directly."

Consider the following code example which gives you hints on how to achieve the same functionality between v1 and v2 Logging:

```diff
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.Logging;
// ... other imports

public class PaymentFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    // BEFORE v2: Uses org.apache.logging.log4j.LogManager
-   private static final Logger LOGGER = LogManager.getLogger(PaymentFunction.class);
    // AFTER v2: Use org.slf4j.LoggerFactory
+   private static final Logger LOGGER = LoggerFactory.getLogger(PaymentFunction.class);

    @Logging
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // ...

        // BEFORE v2: Uses LoggingUtils.appendKey to append custom global keys
        // LoggingUtils was removed!
-       LoggingUtils.appendKey("cardNumber", card.getId());
        // AFTER v2: Uses native SLF4J Mapped Diagnostic Context (MDC)
+       MDC.put("cardNumber", card.getId());

        // Regular logging has not changed
        LOGGER.info("My log message with argument.");

        // Adding custom keys on a specific log message
        // BEFORE v2: No direct way, only supported via LoggingUtils.appendKey and LoggingUtils.removeKey
        // AFTER v2: Extensive support for StructuredArguments
+       LOGGER.info("Collecting payment", StructuredArguments.entry("orderId", order.getId()));
        // { "message": "Collecting payment", ..., "orderId": 123}
        Map<String, String> customKeys = new HashMap<>();
        customKeys.put("paymentId", payment.getId());
        customKeys.put("amount", payment.getAmount);
+       LOGGER.info("Payment successful", StructuredArguments.entries(customKeys));
        // { "message": "Payment successful", ..., "paymentId": 123, "amount": 12.99}
    }
}
```

!!! info "Make sure to learn more about the advanced structured argument serialization features in the [Logging v2 documentation](./core/logging.md/#custom-keys)."

## Updated Metrics utility interface

<!-- - Remove deprecated methods: https://github.com/aws-powertools/powertools-lambda-java/pull/1624/files#diff-0afede8005aa2baeba2770f66d611bf0e8ee3969205be27e803682a7f2d6520a -->
<!-- - Re-designed metrics module: https://github.com/aws-powertools/powertools-lambda-java/issues/1848 -->

The Metrics utility was redesigned to be more modular and allow for the addition of new metrics providers in the future. The same EMF-based metrics logging still applies but will be called via an updated public interface. Consider the following list to understand some of changes:

- `#!java @Metrics` was renamed to `#!java @FlushMetrics`
- `#!java MetricsLogger.metricsLogger()` was renamed to `#!java MetricsFactory.getMetricsInstance()`
- `put*` calls such as `#!java putMetric()` where replaced with `add*` nomenclature such as `#!java addMetric()`
- All direct imports from `software.amazon.cloudwatchlogs.emf` need to be replaced with Powertools counterparts from `software.amazon.lambda.powertools.metrics` (see example below)
- The `withSingleMetric` and `withMetricsLogger` methods were removed in favor of `#!java metrics.flushSingleMetric()`
- It is no longer valid to skip declaration of a namespace. If no namespace is provided, an exception will be raised instead of using the default `aws-embedded-metrics` namespace.

The following example shows a common Lambda handler using the Metrics utility and required refactorings.

```diff
// Metrics is not a decorator anymore but the replacement for the `MetricsLogger` Singleton
import software.amazon.lambda.powertools.metrics.Metrics;
+ import software.amazon.lambda.powertools.metrics.FlushMetrics;
- import software.amazon.lambda.powertools.metrics.MetricsUtils;
+ import software.amazon.lambda.powertools.metrics.MetricsFactory;
- import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
- import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
- import software.amazon.cloudwatchlogs.emf.model.Unit;
+ import software.amazon.lambda.powertools.metrics.model.DimensionSet;
+ import software.amazon.lambda.powertools.metrics.model.MetricUnit;

public class MetricsEnabledHandler implements RequestHandler<Object, Object> {

    // This is still a Singleton
-   MetricsLogger metricsLogger = MetricsUtils.metricsLogger();
+   Metrics metrics = MetricsFactory.getMetricsInstance();

    @Override
-   @Metrics(namespace = "ExampleApplication", service = "booking")
+   @FlushMetrics(namespace = "ExampleApplication", service = "booking")
    public Object handleRequest(Object input, Context context) {
-       metricsLogger.putDimensions(DimensionSet.of("environment", "prod"));
+       metrics.addDimension(DimensionSet.of("environment", "prod"));
        // New method overload for adding 2D dimensions more conveniently
+       metrics.addDimension("environment", "prod");
-       metricsLogger.putMetric("SuccessfulBooking", 1, Unit.COUNT);
+       metrics.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT);
        ...
    }
}
```

Learn more about the redesigned Metrics utility in the [Metrics documentation](./core/metrics.md).

## Deprecated capture mode related `@Tracing` annotation parameters

<!-- - Remove deprecated methods: https://github.com/aws-powertools/powertools-lambda-java/pull/1624/files#diff-9b8ed4ca67e310d3ae90e61e2ceffbfec0402082b5a1f741d467f132e3370a21 -->

The deprecated `captureError` and `captureResponse` arguments to the `@Tracing` annotation were removed in v2 and replaced by a new `captureMode` parameter. The parameter can be passed an Enum value of `CaptureMode`.

You should update your code using the new `captureMode` argument:

```diff
- @Tracing(captureError = false, captureResponse = false)
+ @Tracing(captureMode = CaptureMode.DISABLED)
public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    // ...
}
```

Learn more about valid `CaptureMode` values in the [Tracing documentation](./core/tracing.md).

## Idempotency utility split into sub-modules by provider

The Idempotency utility was split from the common `powertools-idempotency` package into individual packages for different persistence store providers. The main business logic is now in the `powertools-idempotency-core` package.

You should now include the `powertools-idempotency-core` package as an AspectJ library and the provider package like `powertools-idempotency-dynamodb` as a regular dependency.

```diff
<!-- BEFORE v2 -->
- <dependency>
-     <groupId>software.amazon.lambda</groupId>
-     <artifactId>powertools-idempotency</artifactId>
-     <version>1.x.x</version>
- </dependency>
<!-- AFTER v2 -->
<!-- In dependencies section -->
+ <dependency>
+     <groupId>software.amazon.lambda</groupId>
+     <artifactId>powertools-idempotency-dynamodb</artifactId>
+     <version>2.x.x</version>
+ </dependency>
<!-- In AspectJ configuration section -->
+ <aspectLibrary>
+     <groupId>software.amazon.lambda</groupId>
+     <artifactId>powertools-idempotency-core</artifactId>
+ </aspectLibrary>
```

## Parameters utility split into sub-modules by provider

Parameters utilities were split from the common `powertools-parameters` package into individual packages for different parameter providers. You should now include the specific parameters dependency for your provider. If you use multiple providers, you can include multiple packages. Each parameter provider needs to be included as a dependency and an AspectJ library to use annotations.

This new structure reduces the bundle size of your deployment package.

```diff
<!-- BEFORE v2 -->
<!-- In dependencies section -->
- <dependency>
-     <groupId>software.amazon.lambda</groupId>
-     <artifactId>powertools-parameters</artifactId>
-     <version>1.x.x</version>
- </dependency>
<!-- In AspectJ configuration section -->
- <aspectLibrary>
-     <groupId>software.amazon.lambda</groupId>
-     <artifactId>powertools-parameters</artifactId>
- </aspectLibrary>
<!-- AFTER v2 -->
<!-- In dependencies section -->
+ <dependency>
+     <groupId>software.amazon.lambda</groupId>
+     <artifactId>powertools-parameters-secrets</artifactId>
+     <version>2.x.x</version>
+ </dependency>
<!-- ... your other providers -->
<!-- In AspectJ configuration section -->
+ <aspectLibrary>
+     <groupId>software.amazon.lambda</groupId>
+     <artifactId>powertools-parameters-secrets</artifactId>
+ </aspectLibrary>
<!-- ... your other providers -->
```

!!! info "Find the full list of supported providers in the [Parameters utility documentation](./utilities/parameters.md)."

## Custom Resources updates the `Response` class

<!-- - Remove deprecated methods: https://github.com/aws-powertools/powertools-lambda-java/pull/1624/files#diff-0afede8005aa2baeba2770f66d611bf0e8ee3969205be27e803682a7f2d6520a -->

The `Response` class supporting CloudFormation Custom Resource implementations was updated to remove deprecated methods.

The `#!java Response.failed()` and `#!java Response.success()` methods without parameters were removed and require the physical resource ID now. You should update your code to use:

- `#!java Response.failed(String physicalResourceId)`
- `#!java Response.success(String physicalResourceId)`

```diff
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

public class MyCustomResourceHandler extends AbstractCustomResourceHandler {

    // ...

    @Override
    protected Response update(CloudFormationCustomResourceEvent updateEvent, Context context) {
+       String physicalResourceId = updateEvent.getPhysicalResourceId();
        UpdateResult updateResult = doUpdates(physicalResourceId);
        if (updateResult.isSuccessful()) {
-           return Response.success();
+           return Response.success(physicalResourceId);
        } else {
-           return Response.failed();
+           return Response.failed(physicalResourceId);
        }
    }

    // ...
}
```

## Improved integration of Validation utility with other utilities

<!--
- Partial failure batch validation: https://github.com/aws-powertools/powertools-lambda-java/pull/1621
- Return 4xx errors codes with API GW: https://github.com/aws-powertools/powertools-lambda-java/pull/1489
-->

The Validation utility includes two updates that change the behavior of integration with other utilities and AWS services.

**1. Updated HTTP status code when using `@Validation` with API Gateway**

This does not require a code change in the Lambda function using the Validation utility but might impact how your calling application treats exceptions. Prior to `v2`, a 500 HTTP status code was returned when the validation did not pass. Consistent with the [HTTP specification](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status){target="\_blank"}, a 400 status code is returned now indicating a user error instead of a server error.

Consider the following example:

```java
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

If the request validation fails, you can expect the following change in the HTTP response status code on the client-side:

```sh
# BEFORE v2: 500 Internal Server Error
❯ curl -s -o /dev/null -w "%{http_code}" https://{API_ID}.execute-api.{REGION}.amazonaws.com/{STAGE}/{PATH}
500
# AFTER v2: 400 Bad Request
❯ curl -s -o /dev/null -w "%{http_code}" https://{API_ID}.execute-api.{REGION}.amazonaws.com/{STAGE}/{PATH}
400
```

**2. Integration with partial batch failures when using Batch utility**

This does not require a code change but might affect the batch processing flow when using the Validation utility in combination with the Batch processing utility.

Consider the following example:

```java
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import software.amazon.lambda.powertools.batch.handler.BatchMessageHandler;

public class SqsBatchHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final BatchMessageHandler<SQSEvent, SQSBatchResponse> handler;

    public SqsBatchHandler() {
        handler = new BatchMessageHandlerBuilder()
                .withSqsBatchHandler()
                .buildWithMessageHandler(this::processMessage, Product.class);
    }

    @Override
    @Validation(inboundSchema = "classpath:/schema_in.json", outboundSchema = "classpath:/schema_out.json")
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        return handler.processBatch(sqsEvent, context);
    }

    private void processMessage(Product p, Context c) {
        // Process the product
    }
}
```

- **Prior to `v2`** this caused the whole batch to fail.
- **After `v2`** this will add only the failed events to the batch item failure list in the response and process the remaining messages.

!!! info "Check if your workload can tolerate this behavior and make sure it is designed for idempotency when using partial batch item failures. We offer the [Idempotency](./utilities/idempotency.md) utility to simplify integration of idempotent behavior in your workloads."

## AspectJ runtime not included by default anymore

The AspectJ runtime is no longer included as a transitive dependency of Powertools. For all utilities offering annotations using AspectJ compile-time weaving, you need to include the AspectJ runtime yourself now. This is also documented, with a complete example, in our [installation guide](./index.md). For Maven projects, make sure to add the following dependency in your dependencies section:

```diff
+ <dependency>
+     <groupId>org.aspectj</groupId>
+     <artifactId>aspectjrt</artifactId>
+     <version>1.9.22</version>
+ </dependency>
```

## Removed `powertools-sqs` module in favor of `powertools-batch`

The archived documentation contains a migration guide for both large message handling using `powertools-sqs` and batch processing using `powertools-sqs`. The sections below explain the high-level steps for your convenience.

### Migrating SQS Batch processing (`@SqsBatch`)

The [batch processing library](./utilities/batch.md) provides a way to process messages and gracefully handle partial failures for SQS, Kinesis Streams, and DynamoDB Streams batch sources. In comparison to the legacy SQS Batch library, it relies on [Lambda partial batch responses](https://docs.aws.amazon.com/lambda/latest/dg/services-sqs-errorhandling.html#services-sqs-batchfailurereporting){target="\_blank"}, which allows the library to provide a simpler, more reliable interface for processing batches.

In order to get started, check out the new [processing messages from SQS](./utilities/batch.md/#processing-messages-from-sqs) documentation. In most cases, you will simply be able to retain your existing batch message handler function, and wrap it with the new batch processing interface. Unlike the `powertools-sqs` module, the new `powertools-batch` module uses _partial batch responses_ to communicate to Lambda which messages have been processed and must be removed from the queue. The return value of the handler's process function must be returned to Lambda.

The new library also no longer requires the `SQS:DeleteMessage` action on the Lambda function's role policy, as Lambda
itself now manages removal of messages from the queue.

<!-- prettier-ignore-start -->
!!! info "Some tuneables from `powertools-sqs` are no longer provided."
    - **Non-retryable Exceptions** - there is no mechanism to indicate in a partial batch response that a particular message
        should not be retried and instead moved to DLQ - a message either succeeds, or fails and is retried. A message
        will be moved to the DLQ once the normal retry process has expired.
    - **Suppress Exception** - The new batch processor does not throw an exception on failure of a handler. Instead,
        its result must be returned by your code from your message handler to Lambda, so that Lambda can manage
        the completed messages and retry behaviour.
<!-- prettier-ignore-end -->

### Migrating SQS Large message handling (`@SqsLargeMessage`)

- Replace the dependency in Maven / Gradle: `powertools-sqs` ==> `powertools-large-messages`
- Replace the annotation: `@SqsLargeMessage` ==> `@LargeMessage` (the new module handles both SQS and SNS)
- Move the annotation away from the Lambda `handleRequest` method and put it on a method with `SQSEvent.SQSMessage` or `SNSEvent.SNSRecord` as first parameter.
- The annotation now handles a single message, contrary to the previous version that was handling the complete batch. This gives more control, especially when dealing with partial failures with SQS (see the batch module).
- The new module only provides an annotation: an equivalent to the `SqsUtils` class is not available anymore in this new version.
