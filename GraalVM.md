# GraalVM Compatibility for AWS Lambda Powertools Java

## Table of Contents
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [General Implementation Steps](#general-implementation-steps)
- [Known Issues and Solutions](#known-issues-and-solutions)
- [Reference Implementation](#reference-implementation)

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
    - Run tests with `-Pgenerate-graalvm-files` profile.
```shell
mvn -Pgenerate-graalvm-files clean test
```

3. **Validate Native Image Tests**
    - Set the `JAVA_HOME` environment variable to use GraalVM
    - Run tests with `-Pgraalvm-native` profile. This will build a GraalVM native image and run the JUnit tests.
```shell
mvn -Pgraalvm-native clean test
```

4. **Clean Up Metadata**
    -  GraalVM metadata reachability files generated in Step 2 contains references to the test scoped dependencies as well.
    - Remove the references in generated metadata files for the following (and any other references to test scoped resources and classes):
        - JUnit
        - Mockito
        - ByteBuddy

## Known Issues and Solutions
1. **Mockito Compatibility**
   - Powertools uses Mockito 5.x which uses “inline mock maker” as the default. This mock maker does not play well with GraalVM. Mockito [recommends](https://github.com/mockito/mockito/releases/tag/v5.0.0) using subclass mock maker with GraalVM. Therefore `generate-graalvm-files` profile uses subclass mock maker instead of inline mock maker.
   - Subclass mock maker does not support testing static methods. Tests have therefore been modified to use [JUnit Pioneer](https://junit-pioneer.org/docs/environment-variables/) to inject the environment variables in the scope of the test's execution.

2. **Log4j Compatibility**
   - Version 2.22.1 fails with this error
```
java.lang.InternalError: com.oracle.svm.core.jdk.UnsupportedFeatureError: Defining hidden classes at runtime is not supported.
```
   - This has been [fixed](https://github.com/apache/logging-log4j2/discussions/2364#discussioncomment-8950077) in Log4j 2.24.x. PT has been updated to use this version of Log4j 

## Reference Implementation
Working example is available in the [examples](examples/powertools-examples-core-utilities/sam-graalvm). 
