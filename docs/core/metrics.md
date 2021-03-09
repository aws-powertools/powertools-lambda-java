---
title: Metrics
description: Core utility
---

Metrics creates custom metrics asynchronously by logging metrics to standard output following Amazon CloudWatch Embedded Metric Format (EMF).

These metrics can be visualized through [Amazon CloudWatch Console](https://console.aws.amazon.com/cloudwatch/).

**Key features**

* Aggregate up to 100 metrics using a single CloudWatch EMF object (large JSON blob).
* Validate against common metric definitions mistakes (metric unit, values, max dimensions, max metrics, etc).
* Metrics are created asynchronously by the CloudWatch service, no custom stacks needed.
* Context manager to create a one off metric with a different dimension.

## Terminologies

If you're new to Amazon CloudWatch, there are two terminologies you must be aware of before using this utility:

* **Namespace**. It's the highest level container that will group multiple metrics from multiple services for a given application, for example `ServerlessEcommerce`.
* **Dimensions**. Metrics metadata in key-value format. They help you slice and dice metrics visualization, for example `ColdStart` metric by Payment `service`.

<figure>
  <img src="../../media/metrics_terminology.png" />
  <figcaption>Metric terminology, visually explained</figcaption>
</figure>


## Getting started

Metric has two global settings that will be used across all metrics emitted:

Setting | Description | Environment variable | Constructor parameter
------------------------------------------------- | ------------------------------------------------- | ------------------------------------------------- | -------------------------------------------------
**Metric namespace** | Logical container where all metrics will be placed e.g. `ServerlessAirline` |  `POWERTOOLS_METRICS_NAMESPACE` | `namespace`
**Service** | Optionally, sets **service** metric dimension across all metrics e.g. `payment` | `POWERTOOLS_SERVICE_NAME` | `service`

!!! tip "Use your application or main service as the metric namespace to easily group all metrics"

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

    ```java hl_lines="8"
    import software.amazon.lambda.powertools.metrics.Metrics;
    
    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {
    
        MetricsLogger metricsLogger = MetricsUtils.metricsLogger();
    
        @Override
        @Metrics(namespace = "ExampleApplication", service = "booking")
        public Object handleRequest(Object input, Context context) {
            ...
        }
    }
    ```

You can initialize Metrics anywhere in your code as many times as you need - It'll keep track of your aggregate metrics in memory.

## Creating metrics

You can create metrics using `putMetric`, and manually create dimensions for all your aggregate metrics using `putDimensions`.

=== "MetricsEnabledHandler.java"

    ```java hl_lines="11 12"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

    public class MetricsEnabledHandler implements RequestHandler<Object, Object> {
    
        MetricsLogger metricsLogger = MetricsUtils.metricsLogger();
    
        @Override
        @Metrics(namespace = "ExampleApplication", service = "booking")
        public Object handleRequest(Object input, Context context) {
            metricsLogger.putDimensions(DimensionSet.of("environment", "prod"));
            metricsLogger.putMetric("SuccessfulBooking", 1, Unit.COUNT);
            ...
        }
    }
    ```

!!! tip "The `Unit` enum facilitate finding a supported metric unit by CloudWatch."

!!! note "Metrics overflow"
    CloudWatch EMF supports a max of 100 metrics. Metrics utility will flush all metrics when adding the 100th metric while subsequent metrics will be aggregated into a new EMF object, for your convenience.

### Flushing metrics

The `@Metrics` annotation **validates**, **serializes**, and **flushes** all your metrics. During metrics validation, 
if no metrics are provided no exception will be raised. If metrics are provided, and any of the following criteria are 
not met, `ValidationException` exception will be raised.

!!! tip "Metric validation"
    * Maximum of 9 dimensions

If you want to ensure that at least one metric is emitted, you can pass `raiseOnEmptyMetrics = true` to the **@Metrics** annotation:

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

* Create a separate EMF blob solely containing a metric named `ColdStart`
* Add `FunctionName` and `Service` dimensions

This has the advantage of keeping cold start metric separate from your application metrics.

## Advanced

## Adding metadata

You can use `putMetadata` for advanced use cases, where you want to metadata as part of the serialized metrics object.

!!! info
    **This will not be available during metrics visualization, use `dimensions` for this purpose.**

=== "App.java"

    ```java hl_lines="8 9" 
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

    public class App implements RequestHandler<Object, Object> {

        @Override
        @Metrics(namespace = "ServerlessAirline", service = "payment")
        public Object handleRequest(Object input, Context context) {
            metricsLogger().putMetric("CustomMetric1", 1, Unit.COUNT);
            metricsLogger().putMetadata("booking_id", "1234567890");
            ...
        }
    }
    ```

This will be available in CloudWatch Logs to ease operations on high cardinal data.

## Overriding default dimension set

By default, all metrics emitted via module captures `Service` as one of the default dimension. This is either specified via
`POWERTOOLS_SERVICE_NAME` environment variable or via `service` attribute on `Metrics` annotation. If you wish to override the default 
Dimension, it can be done via `#!java MetricsUtils.defaultDimensionSet()`.

=== "App.java"

    ```java hl_lines="8 9 10"
    import software.amazon.lambda.powertools.metrics.Metrics;
    import static software.amazon.lambda.powertools.metrics.MetricsUtils;
    
    public class App implements RequestHandler<Object, Object> {
    
        MetricsLogger metricsLogger = MetricsUtils.metricsLogger();
        
        static {
            MetricsUtils.defaultDimensionSet(DimensionSet.of("CustomDimension", "booking"));
        }
    
        @Override
        @Metrics(namespace = "ExampleApplication", service = "booking")
        public Object handleRequest(Object input, Context context) {
            ...
            MetricsUtils.withSingleMetric("Metric2", 1, Unit.COUNT, log -> {});
        }
    }
    ```

## Creating a metric with a different dimension

CloudWatch EMF uses the same dimensions across all your metrics. Use `withSingleMetric` if you have a metric that should have different dimensions.

!!! info
    Generally, this would be an edge case since you [pay for unique metric](https://aws.amazon.com/cloudwatch/pricing/). Keep the following formula in mind:
    **unique metric = (metric_name + dimension_name + dimension_value)**

=== "App.java"

    ```java hl_lines="7 8 9" 
    import static software.amazon.lambda.powertools.metrics.MetricsUtils.withSingleMetric;

    public class App implements RequestHandler<Object, Object> {

        @Override
        public Object handleRequest(Object input, Context context) {
             withSingleMetric("CustomMetrics2", 1, Unit.COUNT, "Another", (metric) -> {
                metric.setDimensions(DimensionSet.of("AnotherService", "CustomService"));
            });
        }
    }
    ```