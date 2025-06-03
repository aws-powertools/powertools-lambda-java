/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Metrics} is used to signal that the annotated Lambda handler method should be
 * extended with Metrics functionality. Will have no effect when used on a method that is not a Lambda handler.
 *
 * <p>{@code Metrics} allows users to asynchronously create Amazon
 * CloudWatch metrics by using the CloudWatch <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html">Embedded Metrics Format</a>.
 * {@code Metrics} manages the life-cycle and configuration of the MetricsLogger to simplify the user experience when used with AWS Lambda.
 *
 * <p>{@code Metrics} should be used with the handleRequest method of a class
 * which implements either
 * {@code com.amazonaws.services.lambda.runtime.RequestHandler} or
 * {@code com.amazonaws.services.lambda.runtime.RequestStreamHandler}.</p>
 *
 * <p>{@code Metrics} creates Amazon CloudWatch custom metrics. You can find
 * pricing information on the <a href="https://aws.amazon.com/cloudwatch/pricing/">CloudWatch pricing documentation</a> page.</p>
 *
 * <p>To enable creation of custom metrics for cold starts you can add {@code @Metrics(captureColdStart = true)}.
 * </br>This will create a metric with the key {@code "ColdStart"} and the unit type {@code COUNT}.
 * </p>
 *
 * <p>To raise exception if no metrics are emitted, use {@code @Metrics(raiseOnEmptyMetrics = true)}.
 * </br>This will create an exception if no metrics are emitted. By default its value is set to false.
 * </p>
 *
 * <p>By default the service name associated with metrics created will be
 * "service_undefined". This can be overridden with the environment variable {@code POWERTOOLS_SERVICE_NAME}
 * or the annotation variable {@code @Metrics(service = "Service Name")}.
 * If both are specified then the value of the annotation variable will be used.</p>
 *
 * <p>By default the namespace associated with metrics created will be "aws-embedded-metrics".
 * This can be overridden with the environment variable {@code POWERTOOLS_METRICS_NAMESPACE}
 * or the annotation variable {@code @Metrics(namespace = "Namespace")}.
 * If both are specified then the value of the annotation variable will be used.</p>
 * 
 * <p>You can specify a custom function name with {@code @Metrics(functionName = "MyFunction")}.
 * If specified, this will be used instead of the function name from the Lambda context.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Metrics {
    String namespace() default "";

    String service() default "";

    String functionName() default "";

    boolean captureColdStart() default false;

    boolean raiseOnEmptyMetrics() default false;
}
