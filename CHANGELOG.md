# Changelog

All notable changes to this project will be documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format for changes and adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).



## [Unreleased]

## [1.20.1] - 2025-04-08

* docs: fix 2 typos (#1739) by @ntestor
* docs: Correct XML formatting for Maven configuration in Large Messages utility docs (#1796) by @jreijn
* fix: Load version.properties file as resource stream to fix loading when packaged as jar (#1813) by @phipag

## [1.20.0] - 2025-03-25

* feat(cfn-custom-resource): Add optional 'reason' field for detailed failure reporting (#1758) by @moizsh

## [1.19.0] - 2025-03-07

* chore(deps): Update deps for jackson ([#1793](https://github.com/aws-powertools/powertools-lambda-java/pull/1793)) by [@sthulb](https://github.com/sthulb)
* build(deps): bump log4j.version from 2.22.1 to 2.24.3 ([#1777](https://github.com/aws-powertools/powertools-lambda-java/pull/1777)) by [@dependabot](https://github.com/dependabot)
* chore(deps): update JSII to 1.108 ([#1791](https://github.com/aws-powertools/powertools-lambda-java/pull/1791)) by [@sthulb](https://github.com/sthulb)
* build(deps): bump jinja2 from 3.1.5 to 3.1.6 in /docs ([#1789](https://github.com/aws-powertools/powertools-lambda-java/pull/1789)) by [@dependabot](https://github.com/dependabot)
* chore: Update netty version ([#1768](https://github.com/aws-powertools/powertools-lambda-java/pull/1768)) by [@sthulb](https://github.com/sthulb)
* chore: Set versions of transitive dependencies ([#1767](https://github.com/aws-powertools/powertools-lambda-java/pull/1767)) by [@sthulb](https://github.com/sthulb)
* chore: update Jackson in examples ([#1766](https://github.com/aws-powertools/powertools-lambda-java/pull/1766)) by [@sthulb](https://github.com/sthulb)
* build(deps): bump org.apache.maven.plugins:maven-jar-plugin from 3.4.1 to 3.4.2 ([#1731](https://github.com/aws-powertools/powertools-lambda-java/pull/1731)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.xray.recorder.version from 2.15.3 to 2.18.1 ([#1726](https://github.com/aws-powertools/powertools-lambda-java/pull/1726)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.26.29 to 2.27.12 ([#1724](https://github.com/aws-powertools/powertools-lambda-java/pull/1724)) by [@dependabot](https://github.com/dependabot)
* fix: Allow empty responses as well as null response in AppConfig ([#1673](https://github.com/aws-powertools/powertools-lambda-java/pull/1673)) by [@chrisclayson](https://github.com/chrisclayson)
* build(deps): bump aws.sdk.version from 2.27.2 to 2.27.7 ([#1715](https://github.com/aws-powertools/powertools-lambda-java/pull/1715)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.26.29 to 2.27.2 ([#1714](https://github.com/aws-powertools/powertools-lambda-java/pull/1714)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.26 to 2.26.29 ([#1713](https://github.com/aws-powertools/powertools-lambda-java/pull/1713)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.26.25 to 2.26.29 ([#1712](https://github.com/aws-powertools/powertools-lambda-java/pull/1712)) by [@dependabot](https://github.com/dependabot)
* chore: deprecate java1.8 al1 ([#1706](https://github.com/aws-powertools/powertools-lambda-java/pull/1706)) by [@jeromevdl](https://github.com/jeromevdl)
* chore: java 1.8 AL1 is deprecated, fix E2E tests ([#1692](https://github.com/aws-powertools/powertools-lambda-java/pull/1692)) by [@jeromevdl](https://github.com/jeromevdl)
* build(deps): bump aws.sdk.version from 2.26.21 to 2.26.25 ([#1703](https://github.com/aws-powertools/powertools-lambda-java/pull/1703)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.26.3 to 2.26.21 ([#1697](https://github.com/aws-powertools/powertools-lambda-java/pull/1697)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump jackson.version from 2.17.0 to 2.17.2 ([#1696](https://github.com/aws-powertools/powertools-lambda-java/pull/1696)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.commons:commons-lang3 from 3.13.0 to 3.14.0 ([#1694](https://github.com/aws-powertools/powertools-lambda-java/pull/1694)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump commons-io:commons-io from 2.15.1 to 2.16.1 ([#1691](https://github.com/aws-powertools/powertools-lambda-java/pull/1691)) by [@dependabot](https://github.com/dependabot)
* docs: improve tracing doc for sdk instrumentation ([#1687](https://github.com/aws-powertools/powertools-lambda-java/pull/1687)) by [@jeromevdl](https://github.com/jeromevdl)
* docs: fix tracing links for xray ([#1686](https://github.com/aws-powertools/powertools-lambda-java/pull/1686)) by [@jeromevdl](https://github.com/jeromevdl)
* build(deps): bump org.apache.maven.plugins:maven-failsafe-plugin from 3.2.5 to 3.3.0 ([#1679](https://github.com/aws-powertools/powertools-lambda-java/pull/1679)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.69 to 2.26.3 ([#1658](https://github.com/aws-powertools/powertools-lambda-java/pull/1658)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump com.github.spotbugs:spotbugs-maven-plugin from 4.7.3.6 to 4.8.5.0 ([#1657](https://github.com/aws-powertools/powertools-lambda-java/pull/1657)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-checkstyle-plugin from 3.3.0 to 3.4.0 ([#1653](https://github.com/aws-powertools/powertools-lambda-java/pull/1653)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.50 to 2.25.69 ([#1652](https://github.com/aws-powertools/powertools-lambda-java/pull/1652)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-source-plugin from 3.3.0 to 3.3.1 ([#1646](https://github.com/aws-powertools/powertools-lambda-java/pull/1646)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.assertj:assertj-core from 3.25.3 to 3.26.0 ([#1644](https://github.com/aws-powertools/powertools-lambda-java/pull/1644)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.xray.recorder.version from 2.15.1 to 2.15.3 ([#1643](https://github.com/aws-powertools/powertools-lambda-java/pull/1643)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.35 to 2.25.50 ([#1642](https://github.com/aws-powertools/powertools-lambda-java/pull/1642)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump com.amazonaws:aws-lambda-java-events from 3.11.2 to 3.11.4 ([#1597](https://github.com/aws-powertools/powertools-lambda-java/pull/1597)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.24.10 to 2.25.6 ([#1603](https://github.com/aws-powertools/powertools-lambda-java/pull/1603)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-surefire-plugin from 3.1.2 to 3.2.5 ([#1596](https://github.com/aws-powertools/powertools-lambda-java/pull/1596)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.codehaus.mojo:exec-maven-plugin from 3.1.0 to 3.2.0 ([#1585](https://github.com/aws-powertools/powertools-lambda-java/pull/1585)) by [@dependabot](https://github.com/dependabot)
* build(deps-dev): bump software.amazon.awscdk:aws-cdk-lib from 2.100.0 to 2.130.0 ([#1586](https://github.com/aws-powertools/powertools-lambda-java/pull/1586)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump io.burt:jmespath-jackson from 0.5.1 to 0.6.0 ([#1587](https://github.com/aws-powertools/powertools-lambda-java/pull/1587)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.21.0 to 2.24.10 ([#1581](https://github.com/aws-powertools/powertools-lambda-java/pull/1581)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump commons-io:commons-io from 2.13.0 to 2.15.1 ([#1584](https://github.com/aws-powertools/powertools-lambda-java/pull/1584)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.xray.recorder.version from 2.14.0 to 2.15.1 ([#1583](https://github.com/aws-powertools/powertools-lambda-java/pull/1583)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-shade-plugin from 3.5.0 to 3.5.2 ([#1582](https://github.com/aws-powertools/powertools-lambda-java/pull/1582)) by [@dependabot](https://github.com/dependabot)
* build(deps-dev): bump org.yaml:snakeyaml from 2.1 to 2.2 ([#1400](https://github.com/aws-powertools/powertools-lambda-java/pull/1400)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump log4j.version from 2.20.0 to 2.22.1 ([#1547](https://github.com/aws-powertools/powertools-lambda-java/pull/1547)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-artifact-plugin from 3.4.1 to 3.5.0 ([#1485](https://github.com/aws-powertools/powertools-lambda-java/pull/1485)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump com.amazonaws:aws-lambda-java-serialization from 1.1.2 to 1.1.5 ([#1573](https://github.com/aws-powertools/powertools-lambda-java/pull/1573)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.jacoco:jacoco-maven-plugin from 0.8.10 to 0.8.11 ([#1509](https://github.com/aws-powertools/powertools-lambda-java/pull/1509)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aspectj to 1.9.21 for jdk21 ([#1536](https://github.com/aws-powertools/powertools-lambda-java/pull/1536)) by [@jeromevdl](https://github.com/jeromevdl)
* docs: HelloWorldStreamFunction in examples fails with sam ([#1532](https://github.com/aws-powertools/powertools-lambda-java/pull/1532)) by [@jasoniharris](https://github.com/jasoniharris)
* chore: Testing java21 aspectj pre-release ([#1519](https://github.com/aws-powertools/powertools-lambda-java/pull/1519)) by [@scottgerring](https://github.com/scottgerring)
* fix: LargeMessageIdempotentE2ET Flaky ([#1518](https://github.com/aws-powertools/powertools-lambda-java/pull/1518)) by [@scottgerring](https://github.com/scottgerring)
* build(deps): bump software.amazon.payloadoffloading:payloadoffloading-common from 2.1.3 to 2.2.0 ([#1639](https://github.com/aws-powertools/powertools-lambda-java/pull/1639)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-jar-plugin from 3.3.0 to 3.4.1 ([#1638](https://github.com/aws-powertools/powertools-lambda-java/pull/1638)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump jackson.version from 2.15.3 to 2.17.0 ([#1637](https://github.com/aws-powertools/powertools-lambda-java/pull/1637)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.31 to 2.25.35 ([#1629](https://github.com/aws-powertools/powertools-lambda-java/pull/1629)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.16 to 2.25.31 ([#1625](https://github.com/aws-powertools/powertools-lambda-java/pull/1625)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.21.1 to 2.25.26 ([#1622](https://github.com/aws-powertools/powertools-lambda-java/pull/1622)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-failsafe-plugin from 3.1.2 to 3.2.5 ([#1619](https://github.com/aws-powertools/powertools-lambda-java/pull/1619)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump com.fasterxml.jackson.datatype:jackson-datatype-joda from 2.15.2 to 2.17.0 ([#1616](https://github.com/aws-powertools/powertools-lambda-java/pull/1616)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump aws.sdk.version from 2.25.6 to 2.25.16 ([#1613](https://github.com/aws-powertools/powertools-lambda-java/pull/1613)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.apache.maven.plugins:maven-gpg-plugin from 3.1.0 to 3.2.1 ([#1610](https://github.com/aws-powertools/powertools-lambda-java/pull/1610)) by [@dependabot](https://github.com/dependabot)
* build(deps): bump org.assertj:assertj-core from 3.24.2 to 3.25.3 ([#1609](https://github.com/aws-powertools/powertools-lambda-java/pull/1609)) by [@dependabot](https://github.com/dependabot)

## [1.18.0] - 2023-11-16

### Added

* feat: add support for [Lambda Advanced Logging Controls (ALC)](https://docs.aws.amazon.com/lambda/latest/dg/monitoring-cloudwatchlogs.html#monitoring-cloudwatchlogs-advanced) (#1514) by @jeromevdl
* feat: Add support for POWERTOOLS_LOGGER_LOG_EVENT (#1510) by @AlexeySoshin

### Maintenance

* fix: json schema 403 error (#1457) by @jeromevdl
* fix: array jmespath fail in idempotency module (#1420) by @jeromevdl
* chore: java21 support in our build (#1488) by @jeromevdl
* chore: Addition of Warn Message If Invalid Annotation Key While Tracing #1511 (#1512) by @jdoherty
* fix: null namespace should fallback to default namespace (#1506) by @jeromevdl
* fix: get trace id from system property when env var is not set (#1503) by @mriccia
* chore: artifacts size on good branches (#1493) by @jeromevdl
* fix: enforce jackson databind version (#1472) by @jeromevdl
* chore: add missing projects and improve workflow (#1487) by @jeromevdl
* chore: Reporting size of the jars in GitHub comments (#1196) by @jeromevdl
* Deps: Bump third party dependencies to the latest versions.

### Documentation

* docs(customer-reference): add Vertex Pharmaceuticals as a customer reference (#1486) by @scottgerring
* docs: Adding Kotlin example. (#1454) by @jasoniharris
* docs: Terraform example (#1478) by @skal111
* docs: Add Serveless Framework example (#1363) by @AlexeySoshin
* docs: Fix link to SQS large message migration guide (#1422) by @scottgerring
* docs(logging): correct log example keys (#1411) by @walmsles
* docs: Update gradle configuration readme (#1359) by @scottgerring

## [1.17.0] - 2023-08-21

### Added
* Feat: Add Batch Processor module in (#1317) by @scottgerring 
* Feat: Add SNS+SQS large messages module (#1310) by @jeromevdl

### Maintenance
* fix: use default credentials provider for all provided SDK clients in (#1303) by @roamingthings
* Chore: Make request for Logger explicitly for current class in (#1307) by @jreijn 
* Chore: checkstyle formater & linter in (#1316) by @jeromevdl
* Chore: Add powertools specific user-agent-suffix to the AWS SDK v2 clients by @eldimi in (#1306)
* Chore: Add 'v2' branch to build workflows to prepare for v2 work in (#1341) by @scottgerring 
* Deps: Bump third party dependencies to the latest versions.

### Documentation
* Docs: Add maintainers guide in (#1326) by @scottgerring 
* Docs: improve contributing guide in (#1334) by @jeromevdl
* Docs: Improve example documentation in (#1291) by @scottgerring 
* Docs: Add discord + sec disclosure links to readme in (#1311) by @scottgerring 
* Docs: Add external examples from AWS SAM CLI App Templates in (#1318) by @AlexeySoshin 
* Docs: Add CDK example in (#1321) by @AlexeySoshin

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
