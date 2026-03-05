# Automatic Priming for AWS Lambda Powertools Java

## Table of Contents
- [Overview](#overview)
- [Implementation Steps](#general-implementation-steps)
- [Known Issues](#known-issues-and-solutions)
- [Reference Implementation](#reference-implementation)

## Overview
Priming is the process of preloading dependencies and initializing resources during the INIT phase, rather than during the INVOKE phase to further optimize startup performance with SnapStart. 
This is required because Java frameworks that use dependency injection load classes into memory when these classes are explicitly invoked, which typically happens during Lambda’s INVOKE phase.

This documentation provides guidance for automatic class priming in Powertools for AWS Lambda Java modules.


## Implementation Steps
Classes are proactively loaded using Java runtime hooks which are part of the open source [CRaC (Coordinated Restore at Checkpoint) project](https://openjdk.org/projects/crac/).
Implementations across the project use the `beforeCheckpoint()` hook, to prime Snapstart-enabled Java functions via Class Priming.
In order to generate the `classloaded.txt` file for a Java module in this project, follow these general steps.

1. **Add Maven Profile**
    - Add maven test profile with the following VM argument for generating classes loaded files. 
   ```shell
      -Xlog:class+load=info:classesloaded.txt
      ```
    - You can find an example of this in `generate-classesloaded-file` profile in this [pom.xml](powertools-metrics/pom.xml).

2. **Generate classes loaded file**
    - Run tests with `-Pgenerate-classesloaded-file` profile.
   ```shell
   mvn -Pgenerate-classesloaded-file clean test
   ```
   - This will generate a file named `classesloaded.txt` in the target directory of the module.

3. **Cleanup the file**
    - The classes loaded file generated in Step 2 has the format
   `[0.054s][info][class,load] java.lang.Object source: shared objects file` 
   but we are only interested in `java.lang.Object` - the fully qualified class name.
    - To strip the lines to include only the fully qualified class name, 
      Use the following regex to replace with empty string.
      - `^\[[\[\]0-9.a-z,]+ ` (to replace the left part)
      - `( source: )[0-9a-z :/._$-]+` (to replace the right part)

4. **Add file to resources**
    -  Move the cleaned-up file to the corresponding `src/main/resources` directory of the module. See [example](powertools-metrics/src/main/resources/classesloaded.txt).

5. **Register and checkpoint**
    - A class, usually the entry point of the module, should register the CRaC resource in the constructor. [Example](powertools-metrics/src/main/java/software/amazon/lambda/powertools/metrics/MetricsFactory.java)
      - Note that AspectJ aspect is not suitable for this purpose, as it does not work with CRaC.
    - Add the `beforeCheckpoint()` hook in the same class to invoke `ClassPreLoader.preloadClasses()`. The `ClassPreLoader` class is implemented in `powertools-common` module.
    - This will ensure that the classes are already pre-loaded by the Snapstart RESTORE operation leading to a shorter INIT duration.
  

## Known Issues
- This is a manual process at the moment, but it can be automated in the future.
- `classesloaded.txt` file includes test classes as well because the file is generated while running tests. This is not a problem because all the classes that are not found are ignored by `ClassPreLoader.preloadClasses()`. Also `beforeCheckpoint()` hook is not time-sensitive, it only runs once when a new Lambda version gets published. 

## Reference Implementation
Working examples are available in:
- [powertools-metrics](powertools-metrics/src/main/java/software/amazon/lambda/powertools/metrics/MetricsFactory.java) - Automatic priming with `ClassPreLoader`
- [powertools-idempotency-dynamodb](powertools-idempotency/powertools-idempotency-dynamodb/src/main/java/software/amazon/lambda/powertools/idempotency/persistence/dynamodb/DynamoDBPersistenceStore.java) - Invoke priming + Automatic priming 
