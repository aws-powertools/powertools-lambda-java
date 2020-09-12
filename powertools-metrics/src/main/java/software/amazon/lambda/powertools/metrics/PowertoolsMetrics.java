package software.amazon.lambda.powertools.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code PowertoolsMetrics} is used to signal that the annotated method should be
 * extended with PowertoolsMetrics functionality.
 *
 * <p>{@code PowertoolsMetrics} allows users to asynchronously create Amazon
 * CloudWatch metrics by using the CloudWatch <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html">Embedded Metrics Format</a>.
 * {@code PowertoolsMetrics} manages the life-cycle of the MetricsLogger class,
 * to simplify the user experience when used with AWS Lambda.
 *
 * <p>{@code PowertoolsMetrics} should be used with the handleRequest method of a class
 * which implements either
 * {@code com.amazonaws.services.lambda.runtime.RequestHandler} or
 * {@code com.amazonaws.services.lambda.runtime.RequestStreamHandler}.</p>
 *
 * <p>{@code PowertoolsMetrics} creates Amazon CloudWatch custom metrics. You can find
 * pricing information on the <a href="https://aws.amazon.com/cloudwatch/pricing/">CloudWatch pricing documentation</a> page.</p>
 *
 * <p>To enable creation of custom metrics for cold starts you can add {@code @PowertoolsMetrics(captureColdStart = true)}.
 * </br>This will create a metric with the key {@code "ColdStart"} and the unit type {@code COUNT}.
 * </p>
 *
 * <p>By default the service name associated with metrics created will be
 * "service_undefined". This can be overridden with the environment variable {@code POWERTOOLS_SERVICE_NAME}
 * or the annotation variable {@code @PowertoolsMetrics(service = "Service Name")}.
 * If both are specified then the value of the annotation variable will be used.</p>
 *
 * <p>By default the namespace associated with metrics created will be "XXXXXXXXX".
 * This can be overridden with the environment variable {@code POWERTOOLS_METRICS_NAMESPACE}
 * or the annotation variable {@code @PowertoolsMetrics(namespace = "Namespace")}.
 * If both are specified then the value of the annotation variable will be used.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PowertoolsMetrics {
    String namespace() default "";
    String service() default "";
    boolean captureColdStart() default false;
}
