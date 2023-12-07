plugins {
    id("io.freefair.aspectj.post-compile-weaving") version "8.2.2"
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("org.aspectj:aspectjrt:1.9.20.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    aspect("software.amazon.lambda:powertools-tracing:2.0.0-SNAPSHOT")
    aspect("software.amazon.lambda:powertools-logging-log4j:2.0.0-SNAPSHOT")
    aspect("software.amazon.lambda:powertools-metrics:2.0.0-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
}

kotlin {
    jvmToolchain(11)
}