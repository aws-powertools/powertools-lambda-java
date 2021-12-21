# Changelog

All notable changes to this project will be documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format for changes and adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

## [1.9.0] - 2021-12-21

* **Logging**: Upgrade Log4j to version 2.17.0 for [CVE-2021-45105](https://nvd.nist.gov/vuln/detail/CVE-2021-45105)
* **Tracing**: add `Service` annotation. [#654](https://github.com/awslabs/aws-lambda-powertools-java/issues/654)

## [1.8.2] - 2021-12-15

## Security

* Upgrading Log4j to version 2.16.0 for [CVE-2021-45046](https://nvd.nist.gov/vuln/detail/CVE-2021-45046)

## [1.8.1] - 2021-12-10

## Security

* Upgrading Log4j to version 2.15.0 for [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)

## [1.8.0] - 2021-11-05

### Added

* **Powertools Cloudformation module (NEW)**: New module simplifying [AWS Lambda-backed custom resources](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources-lambda.html) written in Java. [#560](https://github.com/awslabs/aws-lambda-powertools-java/pull/560)
* **SQS Large message processing**: Ability to override the default `S3Client` use to fetch payload from S3. [#602](https://github.com/awslabs/aws-lambda-powertools-java/pull/602)

### Regression

* **Logging**: `@Logging` annotation now works with `@Tracing` annotation on `RequestStreamHandler` when used in `logEvent` mode. [#567](https://github.com/awslabs/aws-lambda-powertools-java/pull/567)

## Maintenance

* **deps**: Bump third party dependencies to the latest versions.

## [1.7.3] - 2021-09-14

* **SQS Batch processing**: Ability to move non retryable message to configured dead letter queue(DLQ). [#500](https://github.com/awslabs/aws-lambda-powertools-java/pull/500)

## [1.7.2] - 2021-08-03

* **Powertools All Modules**: Upgrade to the latest(1.14.0) aspectj-maven-plugin which also supports Java 9 and newer versions. 
Users no longer need to depend on [com.nickwongdev](https://mvnrepository.com/artifact/com.nickwongdev/aspectj-maven-plugin/1.12.6) as a workaround. [#489](https://github.com/awslabs/aws-lambda-powertools-java/pull/489)
* **Logging**: Performance optimisation to improve cold start. [#484](https://github.com/awslabs/aws-lambda-powertools-java/pull/484)
* **SQS Batch processing/Large message**: Module now lazy loads default SQS client. [#484](https://github.com/awslabs/aws-lambda-powertools-java/pull/484)

## [1.7.1] - 2021-07-06

* **Powertools All Modules**: Fix static code analysis violations done via [spotbugs](https://github.com/spotbugs/spotbugs) ([#458](https://github.com/awslabs/aws-lambda-powertools-java/pull/458)).

## [1.7.0] - 2021-07-05

### Added

* **Logging**: Support for extracting Correlation id using `@Logging` annotation via `correlationIdPath` attribute and `setCorrelationId()` method in `LoggingUtils`([#448](https://github.com/awslabs/aws-lambda-powertools-java/pull/448)).
* **Logging**: New `clearState` attribute on `@Logging` annotation to clear previously added custom keys upon invocation([#453](https://github.com/awslabs/aws-lambda-powertools-java/pull/453)).

## Maintenance

* **deps**: Bump third party dependencies to the latest versions.

## [1.6.0] - 2021-06-21

### Added

* **Tracing**: Support for Boolean and Number type as value in `TracingUtils.putAnnotation()`([#423](https://github.com/awslabs/aws-lambda-powertools-java/pull/432)).
* **Logging**: API to remove any additional custom key from logger entry using `LoggingUtils.removeKeys()`([#395](https://github.com/awslabs/aws-lambda-powertools-java/pull/395)).

## Maintenance

* **deps**: Bump third party dependencies to the latest versions.

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