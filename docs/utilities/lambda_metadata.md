---
title: Lambda Metadata
description: Utility
---

Lambda Metadata provides idiomatic access to the Lambda Metadata Endpoint (LMDS), eliminating boilerplate code for retrieving execution environment metadata like Availability Zone ID.

## Key features

* Retrieve Lambda execution environment metadata with a single method call
* Automatic caching for the sandbox lifetime, avoiding repeated HTTP calls
* Thread-safe access for concurrent executions (compatible with [Lambda Managed Instances](https://docs.aws.amazon.com/lambda/latest/dg/lambda-managed-instances.html){target="_blank"})
* Automatic [SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html){target="_blank"} cache invalidation via [CRaC](https://openjdk.org/projects/crac/){target="_blank"} integration
* Lightweight with minimal external dependencies, using built-in `HttpURLConnection`
* GraalVM support

## Getting started

### Installation

=== "Maven"

    ```xml hl_lines="3-7"
    <dependencies>
        ...
        <dependency>
            <groupId>software.amazon.lambda</groupId>
            <artifactId>powertools-lambda-metadata</artifactId>
            <version>{{ powertools.version }}</version>
        </dependency>
        ...
    </dependencies>
    ```

=== "Gradle"

    ```groovy hl_lines="6"
        repositories {
            mavenCentral()
        }

        dependencies {
            implementation 'software.amazon.lambda:powertools-lambda-metadata:{{ powertools.version }}'
        }

        sourceCompatibility = 11
        targetCompatibility = 11
    ```

### IAM Permissions

No additional IAM permissions are required. The Lambda Metadata Endpoint is available within the Lambda execution environment and uses a Bearer token provided automatically via environment variables.

### Basic usage

Retrieve metadata using `LambdaMetadataClient.get()`:

=== "App.java"

    ```java hl_lines="1 2 9 10"
    import software.amazon.lambda.powertools.metadata.LambdaMetadata;
    import software.amazon.lambda.powertools.metadata.LambdaMetadataClient;

    public class App implements RequestHandler<Object, String> {

        @Override
        public String handleRequest(Object input, Context context) {
            // Fetch metadata (automatically cached after first call)
            LambdaMetadata metadata = LambdaMetadataClient.get();
            String azId = metadata.getAvailabilityZoneId(); // e.g., "use1-az1"

            return "{\"az\": \"" + azId + "\"}";
        }
    }
    ```

!!! info "At launch, only `availabilityZoneId` is available. The API is designed to support additional metadata fields as LMDS evolves."

### Caching behavior

Metadata is **cached automatically** after the first call. Subsequent calls return the cached value without making HTTP requests.

=== "CachingExample.java"

    ```java hl_lines="9 12"
    import software.amazon.lambda.powertools.metadata.LambdaMetadata;
    import software.amazon.lambda.powertools.metadata.LambdaMetadataClient;

    public class CachingExample implements RequestHandler<Object, String> {

        @Override
        public String handleRequest(Object input, Context context) {
            // First call: fetches from endpoint and caches
            LambdaMetadata metadata = LambdaMetadataClient.get();

            // Subsequent calls: returns cached value (no HTTP call)
            LambdaMetadata metadataAgain = LambdaMetadataClient.get();

            // Both return the same cached instance
            assert metadata == metadataAgain;

            return "{\"az\": \"" + metadata.getAvailabilityZoneId() + "\"}";
        }
    }
    ```

This is safe because metadata (like Availability Zone) never changes during a sandbox's lifetime.

## Advanced

### Eager loading at module level

For predictable latency, fetch metadata at class initialization:

=== "EagerLoadingExample.java"

    ```java hl_lines="7"
    import software.amazon.lambda.powertools.metadata.LambdaMetadata;
    import software.amazon.lambda.powertools.metadata.LambdaMetadataClient;

    public class EagerLoadingExample implements RequestHandler<Object, String> {

        // Fetch during cold start (class loading)
        private static final LambdaMetadata METADATA = LambdaMetadataClient.get();

        @Override
        public String handleRequest(Object input, Context context) {
            // No latency hit here - already cached
            return "{\"az\": \"" + METADATA.getAvailabilityZoneId() + "\"}";
        }
    }
    ```

#### SnapStart considerations

When using [SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html){target="_blank"}, the function may restore in a different Availability Zone. The utility automatically handles this by registering with CRaC to invalidate the cache after restore.

Using the same eager loading pattern above, the cache is automatically invalidated on SnapStart restore, ensuring subsequent calls to `LambdaMetadataClient.get()` return refreshed metadata.

!!! note "For module-level usage with SnapStart, ensure `LambdaMetadataClient` is referenced during initialization so the CRaC hook registers before the snapshot is taken."

### Lambda Managed Instances

For [Lambda Managed Instances](https://docs.aws.amazon.com/lambda/latest/dg/lambda-managed-instances.html){target="_blank"} (multi-threaded concurrency), no changes are needed. The utility uses thread-safe caching with `AtomicReference` to ensure correct behavior across concurrent executions on the same instance.

=== "ManagedInstanceHandler.java"

    ```java hl_lines="9"
    import software.amazon.lambda.powertools.metadata.LambdaMetadata;
    import software.amazon.lambda.powertools.metadata.LambdaMetadataClient;

    public class ManagedInstanceHandler implements RequestHandler<Object, String> {

        @Override
        public String handleRequest(Object input, Context context) {
            // Thread-safe: multiple concurrent invocations safely share cached metadata
            LambdaMetadata metadata = LambdaMetadataClient.get();
            return "{\"az\": \"" + metadata.getAvailabilityZoneId() + "\"}";
        }
    }
    ```

### Error handling

The utility throws `LambdaMetadataException` when the metadata endpoint is unavailable or returns an error:

=== "ErrorHandlingExample.java"

    ```java hl_lines="2 7 14 18 21"
    import software.amazon.lambda.powertools.metadata.LambdaMetadata;
    import software.amazon.lambda.powertools.metadata.exception.LambdaMetadataException;
    import software.amazon.lambda.powertools.metadata.LambdaMetadataClient;
    import software.amazon.lambda.powertools.logging.Logging;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;

    public class ErrorHandlingExample implements RequestHandler<Object, String> {

        private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlingExample.class);

        @Override
        @Logging
        public String handleRequest(Object input, Context context) {
            String az;
            try {
                LambdaMetadata metadata = LambdaMetadataClient.get();
                az = metadata.getAvailabilityZoneId();
            } catch (LambdaMetadataException e) {
                LOG.warn("Could not fetch metadata", entry("statusCode", e.getStatusCode()), entry("error", e.getMessage()));
                az = "unknown";
            }

            return "{\"az\": \"" + az + "\"}";
        }
    }
    ```

## Using with other Powertools utilities

Lambda Metadata integrates seamlessly with other Powertools utilities to enrich your observability data with Availability Zone information.

=== "IntegratedExample.java"

    ```java
    import software.amazon.lambda.powertools.logging.Logging;
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;
    import software.amazon.lambda.powertools.metrics.FlushMetrics;
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsFactory;
    import software.amazon.lambda.powertools.metrics.model.MetricUnit;
    import software.amazon.lambda.powertools.metadata.LambdaMetadata;
    import software.amazon.lambda.powertools.metadata.LambdaMetadataClient;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.slf4j.MDC;

    public class IntegratedExample implements RequestHandler<Object, String> {

        private static final Logger LOG = LoggerFactory.getLogger(IntegratedExample.class);
        private static final Metrics metrics = MetricsFactory.getMetricsInstance();

        @Logging
        @Tracing
        @FlushMetrics(captureColdStart = true)
        @Override
        public String handleRequest(Object input, Context context) {
            LambdaMetadata metadata = LambdaMetadataClient.get();
            String azId = metadata.getAvailabilityZoneId();

            // Add AZ as dimension for all metrics
            metrics.addDimension("availability_zone_id", azId);

            // Add AZ to structured logs
            MDC.put("availability_zone_id", azId);
            LOG.info("Processing request");

            // Add AZ to traces
            TracingUtils.putAnnotation("availability_zone_id", azId);

            // Add metrics
            metrics.addMetric("RequestProcessed", 1, MetricUnit.COUNT);

            return "{\"status\": \"ok\"}";
        }
    }
    ```
