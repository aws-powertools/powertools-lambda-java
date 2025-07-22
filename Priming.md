# Automatic Priming for AWS Lambda Powertools Java

## Table of Contents
- [Overview](#overview)
- [Implementation Steps](#general-implementation-steps)
- [Known Issues](#known-issues-and-solutions)
- [Reference Implementation](#reference-implementation)

## Overview
Priming is the process of preloading dependencies and initializing resources during the INIT phase, rather than during the INVOKE phase to further optimize startup performance with SnapStart. 
This is required because Java frameworks that use dependency injection load classes into memory when these classes are explicitly invoked, which typically happens during Lambda’s INVOKE phase.

This documentation provides guidance for automatic class priming in AWS Lambda Powertools Java modules.


## Implementation Steps
Classes are proactively loaded using Java runtime hooks which are part of the open source CRaC (Coordinated Restore at Checkpoint) project.
This implementation uses this hook, called `beforeCheckpoint()`, to prime SnapStart-enabled Java functions via Class Priming.
In order to generate the `classloaded` files for Powertools java module, follow these general steps.

1. **Add Maven Profile**
    - Add maven test profile with the following VM argument for generating classes loaded files. 
   ```shell
      -Xlog:class+load=info:classesloaded.txt
      ```
    - You can find an example of this in profile `generate-classesloaded-file` in this [pom.xml](powertools-metrics/pom.xml).

2. **Generate classes loaded file**
    - Run tests with `-Pgenerate-classesloaded-file` profile.
   ```shell
   mvn -Pgenerate-classesloaded-file clean test
   ```
   - This will generate a file named `classesloaded.txt` in the target directory of the module.

3. **Cleanup the file**
    - The classes loaded file generated in Step 2 has the format
   `[0.054s][info][class,load] java.lang.Object source: shared objects file` 
   but we are only interested in `java.lang.Object` the fully qualified class name.
    - To strip the lines to include only the fully qualified class name, 
      Use the following regex to replace with empty string.
      - `^\[[\[\]0-9.a-z,]+ ` (to replace the left part)
      - `( source: )[0-9a-z :/._$-]+` (to replace the right part)

4. **Place the file**
    -  Move the cleanedup file containing to the corresponding resources directory of the module. See [example](powertools-metrics/src/resources/classesloaded.txt).

5. **Register and checkpoint**
    - Register the CRaC Resource in the constructor of the main class.
    - Add the `beforeCheckpoint()` hook in the same class to invoke `ClassPreLoader.preloadClasses()`.
    - This will ensure that the classes are loaded during the INIT phase of the Lambda function. See [example](powertools-metrics/src/main/java/software/amazon/lambda/powertools/metrics/MetricsFactory.java)
  

## Known Issues
- This is a manual process at the moment, but it can be automated in the future.
- Because the file is generated while running tests, it includes test classes as well.

## Reference Implementation
Working example is available in the [powertools-metrics](powertools-metrics/src/main/java/software/amazon/lambda/powertools/metrics/MetricsFactory.java). 
