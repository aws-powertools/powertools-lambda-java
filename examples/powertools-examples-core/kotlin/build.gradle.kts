plugins {
    id("io.freefair.aspectj.post-compile-weaving") version "6.6.3"
    kotlin("jvm") version "1.9.10"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    aspect("software.amazon.lambda:powertools-tracing:1.18.0")
    aspect("software.amazon.lambda:powertools-logging:1.18.0")
    aspect("software.amazon.lambda:powertools-metrics:1.18.0")
    testImplementation("junit:junit:4.13.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// If using JDK 11 or higher, use the following instead:
//kotlin {
//    jvmToolchain(11)
//}