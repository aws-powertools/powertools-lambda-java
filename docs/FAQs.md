---
title: FAQs
description: Frequently Asked Questions
---


## How can I use Powertools for AWS Lambda (Java) with Lombok?

Powertools uses `aspectj-maven-plugin` to compile-time weave (CTW) aspects into the project. In case you want to use `Lombok` or other compile-time preprocessor for your project, it is required to change `aspectj-maven-plugin` configuration to enable in-place weaving feature. Otherwise, the plugin will ignore changes introduced by `Lombok` and will use `.java` files as a source. 

To enable in-place weaving feature you need to use following `aspectj-maven-plugin` configuration:

```xml hl_lines="2-6"
<configuration>
    <forceAjcCompile>true</forceAjcCompile> 
    <sources/>
    <weaveDirectories>
        <weaveDirectory>${project.build.directory}/classes</weaveDirectory>
    </weaveDirectories>
    ...
    <aspectLibraries>
        <aspectLibrary>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-logging</artifactId>
        </aspectLibrary>
    </aspectLibraries>
</configuration>
```

## How can I use Powertools for AWS Lambda (Java) with Kotlin projects?

Powertools uses `aspectj-maven-plugin` to compile-time weave (CTW) aspects into the project. When using it with Kotlin projects, it is required to `forceAjcCompile`. 
No explicit configuration should be required for gradle projects. 

To enable `forceAjcCompile` you need to use following `aspectj-maven-plugin` configuration:

```xml hl_lines="2"
<configuration>
    <forceAjcCompile>true</forceAjcCompile> 
    ...
    <aspectLibraries>
        <aspectLibrary>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-logging</artifactId>
        </aspectLibrary>
    </aspectLibraries>
</configuration>
```

