---
title: Metrics
description: Core utility
---

Metrics creates custom metrics asynchronously by logging metrics to standard output following [Amazon CloudWatch Embedded Metric Format (EMF)](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format.html).

These metrics can be visualized through [Amazon CloudWatch Console](https://aws.amazon.com/cloudwatch/).

## Key features

- Aggregate up to 100 metrics using a single [CloudWatch EMF](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html){target="\_blank"} object (large JSON blob)
- Validating your metrics against common metric definitions mistakes (for example, metric unit, values, max dimensions, max metrics)
- Metrics are created asynchronously by the CloudWatch service. You do not need any custom stacks, and there is no impact to Lambda function latency
- Support for creating one off metrics with different dimensions
- GraalVM support

## Terminologies

If you're new to Amazon CloudWatch, there are some terminologies you must be aware of before using this utility:

- **Namespace**. It's the highest level container that will group multiple metrics from multiple services for a given application, for example `ServerlessAirline`.
- **Dimensions**. Metrics metadata in key-value format. They help you slice and dice metrics visualization, for example `ColdStart` metric by `service`.
- **Metric**. It's the name of the metric, for example: `SuccessfulBooking` or `UpdatedBooking`.
- **Unit**. It's a value representing the unit of measure for the corresponding metric, for example: `Count` or `Seconds`.
- **Resolution**. It's a value representing the storage resolution for the corresponding metric. Metrics can be either `Standard` or `High` resolution. Read more about CloudWatch Periods [here](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Resolution_definition).

Visit the AWS documentation for a complete explanation for [Amazon CloudWatch concepts](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html).

<figure>
  <img src="../../media/metrics_terminology.png" />
  <figcaption>Metric terminology, visually explained</figcaption>
</figure>

## Install

=== "Maven"

    ```xml hl_lines="3-7 16 18 24-27"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-metrics</artifactId>
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
                             <artifactId>powertools-metrics</artifactId>
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
            aspect 'software.amazon.lambda:powertools-metrics:{{ powertools.version }}'
        }

        sourceCompatibility = 11
        targetCompatibility = 11
    ```

## Getting started

Metrics has three global settings that will be used across all metrics emitted. Use your application or main service as the metric namespace to easily group all metrics:

| Setting                        | Description                                                                     | Environment variable               | Decorator parameter |
| ------------------------------ | ------------------------------------------------------------------------------- | ---------------------------------- | ------------------- |
| **Metric namespace**           | Logical container where all metrics will be placed e.g. `ServerlessAirline`     | `POWERTOOLS_METRICS_NAMESPACE`     | `namespace`         |
| **Service**                    | Optionally, sets **service** metric dimension across all metrics e.g. `payment` | `POWERTOOLS_SERVICE_NAME`          | `service`           |
| **Function name**              | Function name used as dimension for the cold start metric                       | `POWERTOOLS_METRICS_FUNCTION_NAME` | `functionName`      |
| **Disable Powertools Metrics** | Optionally, disables all Powertools metrics                                     | `POWERTOOLS_METRICS_DISABLED`      | N/A                 |

!!! tip "Use your application or main service as the metric namespace to easily group all metrics"

!!! info "`POWERTOOLS_METRICS_DISABLED` will not disable default metrics created by AWS services."

### Order of Precedence of `MetricsLogger` configuration

The `MetricsLogger` Singleton can be configured by three different interfaces. The following order of precedence applies:

1. `@Metrics` annotation
2. `MetricsLoggerBuilder` using Builder pattern (see [Advanced section](#usage-without-metrics-annotation))
3. Environment variables (recommended)

For most use-cases, we recommend using Environment variables and only overwrite settings in code where needed using either the `@Metrics` annotation or `MetricsLoggerBuilder` if the annotation cannot be used.

=== "template.yaml"

    ```yaml hl_lines="9 10"
    Resources:
        HelloWorldFunction:
            Type: AWS::Serverless::Function
            Properties:
            ...
            Runtime: java8
            Environment:
                Variables:
                    POWERTOOLS_SERVICE_NAME: payment
                    POWERTOOLS_METRICS_NAMESPACE: ServerlessAirline
    ```

=== "MetricsEnabledHandler.java"

    ```java hl_lines="9"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;

    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            // ...
        }
    }
    ```

`MetricsLogger` is implemented as a Singleton to keep track of your aggregate metrics in memory and make them accessible anywhere in your code. To guarantee that metrics are flushed properly the `@Metrics` annotation must be added on the lambda handler.

!!!info "You can use the Metrics utility without the `@Metrics` annotation and flush manually. Read more in the [advanced section below](#usage-without-metrics-annotation)."

## Creating metrics

You can create metrics using `addMetric`, and manually create dimensions for all your aggregate metrics using `addDimension`. Anywhere in your code, you can access the current `MetricsLogger` Singleton using the `MetricsLoggerFactory`.

=== "MetricsEnabledHandler.java"

    ```java hl_lines="13"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
    import software.amazon.lambda.powertools.metrics.model.MetricUnit;

    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.addDimension("environment", "prod");
            metricsLogger.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT);
            // ...
        }
    }
    ```

!!! tip "The `MetricUnit` enum facilitates finding a supported metric unit by CloudWatch."

<!-- prettier-ignore-start -->
!!! note "Metrics dimensions"
    CloudWatch EMF supports a max of 9 dimensions per metric. The Metrics utility will validate this limit when adding dimensions.
<!-- prettier-ignore-end -->

### Adding high-resolution metrics

You can create [high-resolution metrics](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html#high-resolution-metrics)
passing a `#!java MetricResolution.HIGH` to the `addMetric` method. If nothing is passed `#!java MetricResolution.STANDARD` will be used.

=== "HigResMetricsHandler.java"

    ```java hl_lines="3 13"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.model.MetricResolution;

    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            // ...
            metricsLogger.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT, MetricResolution.HIGH);
        }
    }
    ```

<!-- prettier-ignore-start -->
!!! info "When is it useful?"
    High-resolution metrics are data with a granularity of one second and are very useful in several situations such as telemetry, time series, real-time incident management, and others.
<!-- prettier-ignore-end -->

### Adding dimensions

You can add dimensions to your metrics using the `addDimension` method. You can either pass key-value pairs or you can create higher cardinality dimensions using `DimensionSet`.

=== "KeyValueDimensionHandler.java"

    ```java hl_lines="3 13"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.model.MetricResolution;

    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.addDimension("Dimension", "Value");
            metricsLogger.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT);
        }
    }
    ```

=== "HighCardinalityDimensionHandler.java"

    ```java hl_lines="4 13-14"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.model.MetricResolution;
    import software.amazon.lambda.powertools.metrics.model.DimensionSet;

    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            // You can add up to 30 dimensions in a single DimensionSet
            metricsLogger.addDimension(DimensionSet.of("Dimension1", "Value1", "Dimension2", "Value2"));
            metricsLogger.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT);
        }
    }
    ```

### Flushing metrics

The `@Metrics` annotation **validates**, **serializes**, and **flushes** all your metrics. During metrics validation,
if no metrics are provided no exception will be raised. If metrics are provided, and any of the following criteria are
not met, `IllegalStateException` or `IllegalArgumentException` exceptions will be raised.

<!-- prettier-ignore-start -->
!!! tip "Metric validation"
    - Maximum of 30 dimensions (`Service` default dimension counts as a regular dimension)
    - Dimension keys and values cannot be null or empty
    - Metric values must be valid numbers
<!-- prettier-ignore-end -->

If you want to ensure that at least one metric is emitted, you can pass `raiseOnEmptyMetrics = true` to the `@Metrics` annotation:

=== "MetricsRaiseOnEmpty.java"

    ```java hl_lines="6"
    import software.amazon.lambda.powertools.metrics.Metrics;

    public class MetricsRaiseOnEmpty implements RequestHandler<Object, Object> {

        @Override
        @Metrics(raiseOnEmptyMetrics = true)
        public Object handleRequest(Object input, Context context) {
        ...
        }
    }
    ```

## Capturing cold start metric

You can capture cold start metrics automatically with `@Metrics` via the `captureColdStart` variable.

=== "MetricsColdStart.java"

    ```java hl_lines="6"
    import software.amazon.lambda.powertools.metrics.Metrics;

    public class MetricsColdStart implements RequestHandler<Object, Object> {

        @Override
        @Metrics(captureColdStart = true)
        public Object handleRequest(Object input, Context context) {
        ...
        }
    }
    ```

If it's a cold start invocation, this feature will:

- Create a separate EMF blob solely containing a metric named `ColdStart`
- Add `FunctionName` and `Service` dimensions

This has the advantage of keeping cold start metric separate from your application metrics.

You can also specify a custom function name to be used in the cold start metric:

=== "MetricsColdStartCustomFunction.java"

    ```java hl_lines="6"
    import software.amazon.lambda.powertools.metrics.Metrics;

    public class MetricsColdStartCustomFunction implements RequestHandler<Object, Object> {

        @Override
        @Metrics(captureColdStart = true, functionName = "CustomFunction")
        public Object handleRequest(Object input, Context context) {
        ...
        }
    }
    ```

<!-- prettier-ignore-start -->
!!!tip "You can overwrite the default `Service` and `FunctionName` dimensions of the cold start metric"
    Set `#!java @Metrics(captureColdStart = false)` and use the `captureColdStartMetric` method manually:

    ```java hl_lines="6 8"
    public class MetricsColdStartCustomFunction implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(captureColdStart = false)
        public Object handleRequest(Object input, Context context) {
            metricsLogger.captureColdStartMetric(context, DimensionSet.of("CustomDimension", "CustomValue"));
            ...
        }
    }
    ```
<!-- prettier-ignore-end -->

## Advanced

### Adding metadata

You can use `addMetadata` for advanced use cases, where you want to add metadata as part of the serialized metrics object.

<!-- prettier-ignore-start -->
!!! info
    This will not be available during metrics visualization, use Dimensions for this purpose.

!!! info
    Adding metadata with a key that is the same as an existing metric will be ignored
<!-- prettier-ignore-end -->

=== "App.java"

    ```java hl_lines="13"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;

    public class App implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "booking-service")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.addMetric("CustomMetric1", 1, MetricUnit.COUNT);
            metricsLogger.addMetadata("booking_id", "1234567890");  // Needs to be added BEFORE flushing
            ...
        }
    }
    ```

This will be available in CloudWatch Logs to ease operations on high cardinal data.

### Setting default dimensions

By default, all metrics emitted via module captures `Service` as one of the default dimensions. This is either specified via `POWERTOOLS_SERVICE_NAME` environment variable or via `service` attribute on `Metrics` annotation.

If you wish to set custom default dimensions, it can be done via `#!java metricsLogger.setDefaultDimensions()`. You can also use the `MetricsLoggerBuilder` instead of the `MetricsLoggerFactory` to configure **and** retrieve the `MetricsLogger` Singleton at the same time (see `MetricsLoggerBuilder.java` tab).

=== "App.java"

    ```java hl_lines="13"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
    import software.amazon.lambda.powertools.metrics.model.DimensionSet;

    public class App implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.setDefaultDimensions(DimensionSet.of("CustomDimension", "booking", "Environment", "prod"));
            ...
        }
    }
    ```

=== "MetricsLoggerBuilder.java"

    ```java hl_lines="8-10"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
    import software.amazon.lambda.powertools.metrics.model.DimensionSet;

    public class App implements RequestHandler<Object, Object> {

        private static final MetricsLogger metricsLogger = MetricsLoggerBuilder.builder()
            .withDefaultDimensions(DimensionSet.of("CustomDimension", "booking", "Environment", "prod"))
            .build();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.addMetric("CustomMetric1", 1, MetricUnit.COUNT);
            ...
        }
    }
    ```

<!-- prettier-ignore-start -->
!!!note
    Overwriting the default dimensions will also overwrite the default `Service` dimension. If you wish to keep `Service` in your default dimensions, you need to add it manually.
<!-- prettier-ignore-end -->

### Creating a single metric with different configuration

You can create a single metric with its own namespace and dimensions using `flushSingleMetric`:

=== "App.java"

    ```java hl_lines="12-18"
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerFactory;
    import software.amazon.lambda.powertools.metrics.model.DimensionSet;
    import software.amazon.lambda.powertools.metrics.model.MetricUnit;

    public class App implements RequestHandler<Object, Object> {
        private static final MetricsLogger metricsLogger = MetricsLoggerFactory.getMetricsLogger();

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.flushSingleMetric(
                "CustomMetric",
                1,
                MetricUnit.COUNT,
                "CustomNamespace",
                DimensionSet.of("CustomDimension", "value")  // Dimensions are optional
            );
        }
    }
    ```

<!-- prettier-ignore-start -->
!!! info
    Generally, this would be an edge case since you [pay for unique metric](https://aws.amazon.com/cloudwatch/pricing). Keep the following formula in mind:

    **unique metric = (metric_name + dimension_name + dimension_value)**
<!-- prettier-ignore-end -->

### Usage without `@Metrics` annotation

The `MetricsLogger` provides all configuration options via `MetricsLoggerBuilder` in addition to the `@Metrics` annotation. This can be useful if work in an environment or framework that does not leverage the vanilla Lambda `handleRequest` method.

!!!info "The environment variables for Service and Namespace configuration still apply but can be overwritten with `MetricsLoggerBuilder` if needed."

The following example shows how to configure a custom `MetricsLogger` using the Builder pattern. Note that it is necessary to manually flush metrics now.

=== "App.java"

    ```java hl_lines="7-12 19 23"
    import software.amazon.lambda.powertools.metrics.MetricsLogger;
    import software.amazon.lambda.powertools.metrics.MetricsLoggerBuilder;
    import software.amazon.lambda.powertools.metrics.model.DimensionSet;
    import software.amazon.lambda.powertools.metrics.model.MetricUnit;

    public class App implements RequestHandler<Object, Object> {
        // Create and configure a MetricsLogger singleton without annotation
        private static final MetricsLogger customLogger = MetricsLoggerBuilder.builder()
            .withNamespace("ServerlessAirline")
            .withRaiseOnEmptyMetrics(true)
            .withService("payment")
            .build();

        @Override
        public Object handleRequest(Object input, Context context) {
            // You can manually capture the cold start metric
            // Lambda context is an optional argument if not available in your environment
            // Dimensions are also optional.
            customLogger.captureColdStartMetric(context, DimensionSet.of("FunctionName", "MyFunction", "Service", "payment"));

            // Add metrics to the custom logger
            customLogger.addMetric("CustomMetric", 1, MetricUnit.COUNT);
            customLogger.flush();
        }
    }
    ```
