# GraalVM Compatibility for AWS Lambda Powertools Java

## Table of Contents
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [General Implementation Steps](#general-implementation-steps)
- [Known Issues and Solutions](#known-issues-and-solutions)
- [Reference Implementation](#reference-implementation)
- [Module-Specific Implementation](#module-specific-implementation)
   - [1. Powertools Common](#1-powertools-common)
   - [2. Powertools Logging](#2-powertools-logging)
      - [2.1 Powertools Logging (log4j)](#21-powertools-logging-log4j)
      - [2.2 Powertools Logging (logback)](#22-powertools-logging-logback)
   - [3. Powertools Metrics](#3-powertools-metrics)

## Overview
This documentation provides guidance for adding GraalVM support for AWS Lambda Powertools Java modules and using the modules in Lambda functions.

## Prerequisites
- GraalVM 21+ installation
- Maven 3.x

## General Implementation Steps
GraalVM native image compilation requires complete knowledge of an application's dynamic features at build time. The GraalVM reachability metadata (GRM) JSON files are essential because Java applications often use features that are determined at runtime, such as reflection, dynamic proxy classes, resource loading, and JNI (Java Native Interface). The metadata files tell GraalVM which classes need reflection access, which resources need to be included in the native image, and which proxy classes need to be generated.

In order to generate the metadata reachability files for Powertools for Lambda, follow these general steps.

1. **Add Maven Profiles**
    - Add profile for generating GraalVM reachability metadata files. You can find an example of this in profile `generate-graalvm-files` of this [pom.xml](powertools-common/pom.xml). 
    - Add another profile for running the tests in the native image. You can find and example of this in profile `graalvm-native` of this [pom.xml](powertools-common/pom.xml).

2. **Generate Reachability Metadata**
    - Set the `JAVA_HOME` environment variable to use GraalVM
    - Run tests with `-Pgenerate-graalvm-files` profile. You can find module specific commands in the [Module-Specific Implementation](#module-specific-implementation) section
    - Some tests may need to be skipped depending on the module

3. **Validate Native Image Tests**
    - Set the `JAVA_HOME` environment variable to use GraalVM
    - Run tests with `-Pgraalvm-native` profile. This will build a GraalVM native image and run the JUnit tests. You can find module specific commands in the [Module-Specific Implementation](#module-specific-implementation) section
    - Verify test execution in native mode

4. **Clean Up Metadata**
    -  GraalVM metadata reachability files generated in Step 2 contains references to the test scoped dependencies as well.
    - Remove the references in generated metadata files for the following (and any other references to test scoped resources and classes):
        - JUnit
        - Mockito
        - ByteBuddy

## Known Issues and Solutions

1. **Mockito Compatibility**
   - Powertools uses Mockito 5.x which uses “inline mock maker” as the default. This mock maker does not play well with GraalVM. Mockito [recommends](https://github.com/mockito/mockito/releases/tag/v5.0.0) using subclass mock maker with GraalVM.
   - However, subclass mock maker does not support testing static methods. Some test cases in Powertools uses inline mock maker to mock static methods. These tests have to be skipped while generating the GraalVM reachability metadata files. 
   - This obviously affects the coverage and possibility of missing a required entry in GRM files. At this point we are relying on community to report any missing entries and will update the GRM based on the reports.
   - This issue remains open until ability of test static methods in Mockito 5.x/inline mock maker is available.

2. **Log4j Compatibility**
   - Version 2.22.1 fails with this error
```
java.lang.InternalError: com.oracle.svm.core.jdk.UnsupportedFeatureError: Defining hidden classes at runtime is not supported.
```
   - This has been [fixed](https://github.com/apache/logging-log4j2/discussions/2364#discussioncomment-8950077) in Log4j 2.24.0. PT has been updated to use this version of Log4j 


## Reference Implementation
Working example is available in the [examples](examples/powertools-examples-core-utilities/sam-graalvm). 

## Module-Specific Implementation
Due to the Mockito issues described in the [Known Issues and Solutions](#known-issues-and-solutions) section, some tests needs to be skipped when generating the GRM files. This section shows the commands that need to be used for the modules.   

### 1. Powertools Common

Generate metadata files
```shell

mvn \
-Dtest=\
\!UserAgentConfiguratorTest#testGetVersionFromProperties_InvalidFile,\
\!UserAgentConfiguratorTest#testGetVersion,\
\!UserAgentConfiguratorTest#testGetUserAgent_SetAWSExecutionEnv,\
\!LambdaHandlerProcessorTest#serviceName_Undefined,\
\!LambdaHandlerProcessorTest#serviceName,\
\!LambdaHandlerProcessorTest#isSamLocal,\
\!LambdaHandlerProcessorTest#getXrayTraceId_present,\
\!LambdaHandlerProcessorTest#getXrayTraceId_notPresent \
 -Pgenerate-graalvm-files clean test
```

Run native tests
```shell

mvn \
-Dtest=\
\!UserAgentConfiguratorTest#testGetVersionFromProperties_InvalidFile,\
\!UserAgentConfiguratorTest#testGetVersion,\
\!UserAgentConfiguratorTest#testGetUserAgent_SetAWSExecutionEnv,\
\!LambdaHandlerProcessorTest#serviceName_Undefined,\
\!LambdaHandlerProcessorTest#serviceName,\
\!LambdaHandlerProcessorTest#isSamLocal,\
\!LambdaHandlerProcessorTest#getXrayTraceId_present,\
\!LambdaHandlerProcessorTest#getXrayTraceId_notPresent \
 -Pgraalvm-native clean test
 
```

### 2. Powertools Logging

Generate metadata files
```shell

mvn  -Dtest=\!LambdaLoggingAspectTest#shouldLogxRayTraceIdEnvVarSet,\
\!LambdaLoggingAspectTest#shouldLogxRayTraceIdSystemPropertySet \
 -Pgenerate-graalvm-files clean test

```

Run native tests
```shell

mvn  -Dtest=\!LambdaLoggingAspectTest#shouldLogxRayTraceIdEnvVarSet,\
\!LambdaLoggingAspectTest#shouldLogxRayTraceIdSystemPropertySet \
 -Pgraalvm-native clean test

```
#### 2.1 Powertools Logging (log4j)

Generate metadata files
```shell

mvn  -Dtest=\!PowertoolsResolverTest#shouldResolveRegion \
 -Pgenerate-graalvm-files clean test

```

Run native tests
```shell

mvn  -Dtest=\!PowertoolsResolverTest#shouldResolveRegion \
-Pgraalvm-native clean test

```

#### 2.2 Powertools Logging (logback)

Generate metadata files
```shell

mvn -Dtest=\!MetricsLoggerTest#shouldLogxRayTraceIdEnvVarSet,\
\!MetricsLoggerTest#shouldLogxRayTraceIdSystemPropertySet \
 -Pgenerate-graalvm-files clean test

```

Run native tests
```shell

mvn -Dtest=\!MetricsLoggerTest#shouldLogxRayTraceIdEnvVarSet,\
\!MetricsLoggerTest#shouldLogxRayTraceIdSystemPropertySet \
 -Pgraalvm-native clean test
```

### 3. Powertools Metrics
* All tests need to mock static methods. 
* Comment out the references to `mockStatic` in the JUnits and set the env variables explicitly
* Also pass the system property via mvn build cli

Generate metadata files
```shell

export AWS_EMF_ENVIRONMENT="Lambda"
export _X_AMZN_TRACE_ID="Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1"
export POWERTOOLS_METRICS_NAMESPACE="GlobalName"

mvn -Dcom.amazonaws.xray.traceHeader="Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1" \
 -Pgenerate-graalvm-files clean test
```

Run native tests
```shell

export AWS_EMF_ENVIRONMENT="Lambda"
export _X_AMZN_TRACE_ID="Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1"
export POWERTOOLS_METRICS_NAMESPACE="GlobalName"

mvn -Dcom.amazonaws.xray.traceHeader="Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1" \
 -Pgraalvm-native clean test

```