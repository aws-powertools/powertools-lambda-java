# Changelog

All notable changes to this project will be documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format for changes and adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

## [1.16.1] - 2023-07-19

* Fix: idempotency timeout bug (#1285) by @scottgerring
* Fix: ParamManager cannot provide default SSM & Secrets providers (#1282) by @jeromevdl
* Fix: Handle batch failures in FIFO queues correctly (#1183) by @scottgerring
* Deps: Bump third party dependencies to the latest versions.


## [1.16.0] - 2023-06-29


### Added
* Feature: Add AppConfig provider to parameters module (#1104) by @scottgerring

### Maintenance
* Fix: missing idempotency key should not persist any data (#1201) by @jeromevdl
* Fix:Removing env var credentials provider as default. (#1161) by @msailes
* Chore: Swap implementation of `aspectj-maven-plugin` to support Java 17 (#1172) by @mriccia
* Test: end-to-end tests for core modules and idempotency (#970) by @jeromevdl
* Chore: cleanup spotbugs maven profiles (#1236) by @jeromevdl
* Chore: removing logback from all components (#1227) by @jeromevdl
* Chore: Roll SLF4J log4j bindings to v2 (#1190) by @scottgerring
* Deps: Bump third party dependencies to the latest versions.


## [1.15.0] - 2023-03-20

### Added
* Feature: Add DynamoDB provider to parameters module (#1091) by @scottgerring
* Feature: Update to powertools-cloudformation to deprecate `Response.success()` and `Response.failed()` methods. New helper methods are added to make it easier to follow best practices `Response.success(String physicalResourceId)` and `Response.failed(String physicalResourceId)`. For a detailed explanation please read the [powertools-cloudformation documentation page](https://docs.powertools.aws.dev/lambda-java/utilities/custom_resources/). (#1082) by @msailes
* Update how a Lambda request handler method is identified (#1058) by @humanzz

### Maintenance
* Deps: Bump third party dependencies to the latest versions.
* Examples: Import examples from aws-samples/aws-lambda-powertools-examples (#1051) by @scottgerring
* Deprecate withMetricLogger in favor of withMetricsLogger (#1060) by @humanzz
* Update issue templates (#1062) by @machafer
* Send code coverage report (jacoco) to codecov (#1094) by @jeromevdl

### Documentation

* Improve `powertools-cloudformation` docs (#1090) by @mriccia
* Add link to Powertools for AWS Lambda (Java) workshop (#1095) by @scottgerring
* Fix mdocs and git revision plugin integration (#1066) by @machafer


## [1.14.0] - 2023-02-17

### Added

* Feature: Introduce `MetricsUtils.withMetricsLogger()` utility method (#1000) by @humanzz

#### Maintenance

* Update logic for recording documentation pages views to use correct runtime name (#1047) by @kozub
* Deps: Bump third party dependencies to the latest versions.

### Documentation

* Docs: Update Powertools for AWS Lambda (Java) definition by @heitorlessa
* Docs: Add information about other supported langauges to README and docs (#1033) by @kozub

## [1.13.0] - 2022-12-14

### Added

* Feature: Idempotency - Handle Lambda timeout scenarios for INPROGRESS records (#933) by @jeromevdl

### Bug Fixes

* Fix: Envelope is not taken into account with built-in types (#960) by @jeromevdl
* Fix: Code suggestion from CodeGuru (#984) by @kozub
* Fix: Compilation warning with SqsLargeMessageAspect on gradle (#998) by @jeromevdl
* Fix: Log message processing exceptions as occur (#1011) by @nem0-97

### Documentation

* Docs: Add missing grammar article (#976) by @fsmiamoto

## [1.12.3] - 2022-07-12

#### Maintenance

* Fixes to resolve vulnerable transitive dependencies ([919](https://github.com/aws-powertools/powertools-lambda-java/issues/919))


## [1.12.2] - 2022-04-29

### Bug Fixes

* **SQS Large message processing**: Classpath conflict on `PayloadS3Pointer` when consumer application depends on `payloadoffloading-common`, introduced in [v1.8.0](https://github.com/aws-powertools/powertools-lambda-java/releases/tag/v1.8.0). ([#851](https://github.com/aws-powertools/powertools-lambda-java/pull/851))


## [1.12.1] - 2022-04-21

### Bug Fixes

* **Idempotency**: thread-safety issue of MessageDigest ([#817](https://github.com/aws-powertools/powertools-lambda-java/pull/817)) 
* **Idempotency**: disable dynamodb client creation in persistent store when disabling idempotency ([#796](https://github.com/aws-powertools/powertools-lambda-java/pull/796))


### Maintenance

* **deps**: Bump third party dependencies to the latest versions.


## [1.12.0] - 2022-03-01

### Added
 * **Easy Event Deserialization**: Extraction and deserialization of the main content of events (body, messages, ...) [#757](https://github.com/aws-powertools/powertools-lambda-java/pull/757)

### Bug Fixes
 * Different behavior while using SSMProvider with or without trailing slash in parameter names [#758](https://github.com/aws-powertools/powertools-lambda-java/issues/758)


## [1.11.0] - 2022-02-16

### Added
 * Powertools for AWS Lambda (Java) Idempotency module: New module to get your Lambda function [Idempotent](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/) (#717)
 * Powertools for AWS Lambda (Java) Serialization module: New module to handle JSON (de)serialization (Jackson ObjectMapper, JMESPath functions)


## [1.10.3] - 2022-02-01

### Bug Fixes

* **SQS Batch processing**: Prevent message to be marked as success if failed sending to DLQ for non retryable exceptions. [#731](https://github.com/aws-powertools/powertools-lambda-java/pull/731)

### Documentation


* **SQS Batch processing**: Improve [documentation](https://docs.powertools.aws.dev/lambda-java/utilities/batch/#iam-permissions) on IAM premissions required by function when using utility with an encrypted SQS queue with customer managed KMS keys.


## [1.10.2] - 2022-01-07

* **Tracing**: Ability to override object mapper used for serializing method response as trace metadata when enabled. This provides users ability to customize how and what you want to capture as metadata from method response object. [#698](https://github.com/aws-powertools/powertools-lambda-java/pull/698)

## [1.10.1] - 2022-01-06

* **Logging**: Upgrade Log4j to version 2.17.1 for [CVE-2021-44832](https://nvd.nist.gov/vuln/detail/CVE-2021-44832)

## [1.10.0] - 2021-12-27

* **Logging**: Modern log4j configuration to customise structured logging. Refer [docs](https://docs.powertools.aws.dev/lambda-java/core/logging/#upgrade-to-jsontemplatelayout-from-deprecated-lambdajsonlayout-configuration-in-log4j2xml) to start using new config. [#670](https://github.com/aws-powertools/powertools-lambda-java/pull/670)
* **SQS Batch**: Support batch size greater than 10. [#667](https://github.com/aws-powertools/powertools-lambda-java/pull/667)

## [1.9.0] - 2021-12-21

* **Logging**: Upgrade Log4j to version 2.17.0 for [CVE-2021-45105](https://nvd.nist.gov/vuln/detail/CVE-2021-45105)
* **Tracing**: add `Service` annotation. [#654](https://github.com/aws-powertools/powertools-lambda-java/issues/654)

## [1.8.2] - 2021-12-15

## Security

* Upgrading Log4j to version 2.16.0 for [CVE-2021-45046](https://nvd.nist.gov/vuln/detail/CVE-2021-45046)

## [1.8.1] - 2021-12-10

## Security

* Upgrading Log4j to version 2.15.0 for [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)

## [1.8.0] - 2021-11-05

### Added

* **Powertools for AWS Lambda (Java) Cloudformation module (NEW)**: New module simplifying [AWS Lambda-backed custom resources](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources-lambda.html) written in Java. [#560](https://github.com/aws-powertools/powertools-lambda-java/pull/560)
* **SQS Large message processing**: Ability to override the default `S3Client` use to fetch payload from S3. [#602](https://github.com/aws-powertools/powertools-lambda-java/pull/602)

### Regression

* **Logging**: `@Logging` annotation now works with `@Tracing` annotation on `RequestStreamHandler` when used in `logEvent` mode. [#567](https://github.com/aws-powertools/powertools-lambda-java/pull/567)

### Maintenance

* **deps**: Bump third party dependencies to the latest versions.

## [1.7.3] - 2021-09-14

* **SQS Batch processing**: Ability to move non retryable message to configured dead letter queue(DLQ). [#500](https://github.com/aws-powertools/powertools-lambda-java/pull/500)

## [1.7.2] - 2021-08-03

* **Powertools for AWS Lambda (Java) All Modules**: Upgrade to the latest(1.14.0) aspectj-maven-plugin which also supports Java 9 and newer versions. 
Users no longer need to depend on [com.nickwongdev](https://mvnrepository.com/artifact/com.nickwongdev/aspectj-maven-plugin/1.12.6) as a workaround. [#489](https://github.com/aws-powertools/powertools-lambda-java/pull/489)
* **Logging**: Performance optimisation to improve cold start. [#484](https://github.com/aws-powertools/powertools-lambda-java/pull/484)
* **SQS Batch processing/Large message**: Module now lazy loads default SQS client. [#484](https://github.com/aws-powertools/powertools-lambda-java/pull/484)

## [1.7.1] - 2021-07-06

* **Powertools for AWS Lambda (Java) All Modules**: Fix static code analysis violations done via [spotbugs](https://github.com/spotbugs/spotbugs) ([#458](https://github.com/aws-powertools/powertools-lambda-java/pull/458)).

## [1.7.0] - 2021-07-05

### Added

* **Logging**: Support for extracting Correlation id using `@Logging` annotation via `correlationIdPath` attribute and `setCorrelationId()` method in `LoggingUtils`([#448](https://github.com/aws-powertools/powertools-lambda-java/pull/448)).
* **Logging**: New `clearState` attribute on `@Logging` annotation to clear previously added custom keys upon invocation([#453](https://github.com/aws-powertools/powertools-lambda-java/pull/453)).

### Maintenance

* **deps**: Bump third party dependencies to the latest versions.

## [1.6.0] - 2021-06-21

### Added

* **Tracing**: Support for Boolean and Number type as value in `TracingUtils.putAnnotation()`([#423](https://github.com/aws-powertools/powertools-lambda-java/pull/432)).
* **Logging**: API to remove any additional custom key from logger entry using `LoggingUtils.removeKeys()`([#395](https://github.com/aws-powertools/powertools-lambda-java/pull/395)).

### Maintenance

* **deps**: Bump third party dependencies to the latest versions.

## [1.5.0] - 2021-03-30

* **Metrics**: Ability to set multiple dimensions as default dimensions via `MetricsUtils.defaultDimensions()`. 
  Introduced in [v1.4.0](https://github.com/aws-powertools/powertools-lambda-java/releases/tag/v1.4.0) 
  `MetricsUtils.defaultDimensionSet()` is deprecated now for better user experience.

## [1.4.0] - 2021-03-11
* **Metrics**: Ability to set default dimension for metrics via `MetricsUtils.defaultDimensionSet()`.
  
  **Note**: If your monitoring depends on [default dimensions](https://github.com/awslabs/aws-embedded-metrics-java/blob/main/src/main/java/software/amazon/cloudwatchlogs/emf/logger/MetricsLogger.java#L173) captured before via [aws-embedded-metrics-java](https://github.com/awslabs/aws-embedded-metrics-java), 
  those either need to be updated or has to be explicitly captured via `MetricsUtils.defaultDimensionSet()`.
  

* **Metrics**: Remove validation of having minimum one dimension. EMF now support [Dimension set being empty](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html) as well.

## [1.3.0] - 2021-03-05

* **Powertools**: It now works out of the box with [code guru profile handler implementation](https://docs.aws.amazon.com/codeguru/latest/profiler-ug/lambda-custom.html).
* **Logging**: Ability to override object mapper used for logging event. This provides customers ability to customize how and what they want to log from event.
* **Metrics**: Module now by default captures AWS Request id as property if used together with Metrics annotation. It will also capture Xray Trace ID as property if tracing is enabled. This ensures good observability and tracing.
* **Metrics**:`withSingleMetric` from `MetricsUtils can now pick the default namespace specified either on Metrics annotation or via POWERTOOLS_METRICS_NAMESPACE env var, without need to specify explicitly for each call.
* **Metrics**:`Metrics` annotation captures metrics even in case of unhandled exception from Lambda function.
* **Docs**: Migrated from Gatsby to MKdocs documentation system
