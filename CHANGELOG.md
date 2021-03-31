# Changelog

All notable changes to this project will be documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format for changes and adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

## [1.5.0] - 2021-03-30

* **Metrics**: Ability to set multiple dimensions as default dimensions via `MetricsUtils.defaultDimensions()`. 
  Introduced in [v1.4.0](https://github.com/awslabs/aws-lambda-powertools-java/releases/tag/v1.4.0) 
  `MetricsUtils.defaultDimensionSet()` is deprecated now for better user experience.

## [1.4.0] - 2021-03-11
* **Metrics**: Ability to set default dimension for metrics via `MetricsUtils.defaultDimensionSet()`.
  
  **Note**: If your monitoring depends on [default dimensions](https://github.com/awslabs/aws-embedded-metrics-java/blob/master/src/main/java/software/amazon/cloudwatchlogs/emf/logger/MetricsLogger.java#L173) captured before via [aws-embedded-metrics-java](https://github.com/awslabs/aws-embedded-metrics-java), 
  those either need to be updated or has to be explicitly captured via `MetricsUtils.defaultDimensionSet()`.
  

* **Metrics**: Remove validation of having minimum one dimension. EMF now support [Dimension set being empty](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html) as well.

## [1.3.0] - 2021-03-05

* **Powertools**: It now works out of the box with [code guru profile handler implementation](https://docs.aws.amazon.com/codeguru/latest/profiler-ug/lambda-custom.html).
* **Logging**: Ability to override object mapper used for logging event. This provides customers ability to customize how and what they want to log from event.
* **Metrics**: Module now by default captures AWS Request id as property if used together with Metrics annotation. It will also capture Xray Trace ID as property if tracing is enabled. This ensures good observability and tracing.
* **Metrics**:`withSingleMetric` from `MetricsUtils can now pick the default namespace specified either on Metrics annotation or via POWERTOOLS_METRICS_NAMESPACE env var, without need to specify explicitly for each call.
* **Metrics**:`Metrics` annotation captures metrics even in case of unhandled exception from Lambda function.
* **Docs**: Migrated from Gatsby to MKdocs documentation system