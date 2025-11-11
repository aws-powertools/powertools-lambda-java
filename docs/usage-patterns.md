---
title: Usage patterns
description: Getting to know the Powertools for AWS Lambda toolkit
---

<!-- markdownlint-disable MD043 -->

Powertools for AWS Lambda (Java) is a collection of utilities designed to help you build serverless applications on AWS.

The toolkit is modular, so you can pick and choose the utilities you need for your application, but also combine them for a complete solution for your serverless applications.

## Patterns

Many of the utilities provided can be used with different patterns, depending on your preferences and the structure of your code.

### AspectJ Annotation

If you prefer using annotations to apply cross-cutting concerns to your Lambda handlers, the AspectJ annotation pattern is a good fit. This approach lets you decorate methods with Powertools utilities using annotations, applying their functionality with minimal code changes.

This pattern works well when you want to keep your business logic clean and separate concerns using aspect-oriented programming.

<!-- prettier-ignore -->
!!! note
    This approach requires configuring AspectJ compile-time weaving in your build tool (Maven or Gradle). See the [installation guide](./index.md#install) for setup instructions.

=== "Logging"

    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import software.amazon.lambda.powertools.logging.CorrelationIdPaths;
    import software.amazon.lambda.powertools.logging.Logging;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        private static final Logger log = LoggerFactory.getLogger(App.class);

        @Logging(logEvent = true, correlationIdPath = CorrelationIdPaths.API_GATEWAY_REST)
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            log.info("Processing request");
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Success");
        }
    }
    ```

=== "Metrics"

    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.metrics.FlushMetrics;
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsFactory;
    import software.amazon.lambda.powertools.metrics.model.MetricUnit;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        private static final Metrics metrics = MetricsFactory.getMetricsInstance();

        @FlushMetrics(namespace = "ServerlessApp", service = "payment")
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            metrics.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Success");
        }
    }
    ```

=== "Tracing"

    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.tracing.Tracing;
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

        @Tracing
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.putAnnotation("operation", "payment");
            return processPayment();
        }

        @Tracing
        private APIGatewayProxyResponseEvent processPayment() {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Success");
        }
    }
    ```

### Functional Approach

If you prefer a more functional programming style or want to avoid AspectJ configuration, you can use the Powertools for AWS Lambda (Java) utilities directly in your code. This approach is more explicit and provides full control over how the utilities are applied.

This pattern is ideal when you want to avoid AspectJ setup or prefer a more imperative style. It also eliminates the AspectJ runtime dependency, making your deployment package more lightweight.

=== "Logging"

    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import software.amazon.lambda.powertools.logging.CorrelationIdPaths;
    import software.amazon.lambda.powertools.logging.PowertoolsLogging;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        private static final Logger log = LoggerFactory.getLogger(App.class);

        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            return PowertoolsLogging.withLogging(
                    context,
                    0.7,
                    CorrelationIdPaths.API_GATEWAY_REST,
                    input,
                    () -> processRequest(input));
        }

        private APIGatewayProxyResponseEvent processRequest(APIGatewayProxyRequestEvent input) {
            // do something with input
            log.info("Processing request");
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Success");
        }
    }
    ```

=== "Metrics"

    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.metrics.Metrics;
    import software.amazon.lambda.powertools.metrics.MetricsFactory;
    import software.amazon.lambda.powertools.metrics.model.MetricUnit;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        private static final Metrics metrics = MetricsFactory.getMetricsInstance();

        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            try {
                metrics.addMetric("SuccessfulBooking", 1, MetricUnit.COUNT);
                return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Success");
            } finally {
                metrics.flush();
            }
        }
    }
    ```

=== "Tracing"

    ```java
    import com.amazonaws.services.lambda.runtime.Context;
    import com.amazonaws.services.lambda.runtime.RequestHandler;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
    import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
    import software.amazon.lambda.powertools.tracing.TracingUtils;

    public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
            TracingUtils.withSubsegment("processPayment", subsegment -> {
                subsegment.putAnnotation("operation", "payment");
                // Business logic here
            });
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Success");
        }
    }
    ```

<!-- prettier-ignore -->
!!! note
    The functional approach is available for all utilities. Further examples and detailed usage can be found in the individual documentation pages for each utility.
