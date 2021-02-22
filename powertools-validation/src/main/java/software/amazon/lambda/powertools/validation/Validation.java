/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.validation;

import com.amazonaws.services.lambda.runtime.Context;
import com.networknt.schema.SpecVersion.VersionFlag;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.networknt.schema.SpecVersion.VersionFlag.V7;

/**
 * {@link Validation} is used to specify that the annotated method input and/or output needs to be valid.<br>
 *
 * <p>{@link Validation} should be used on the {@link com.amazonaws.services.lambda.runtime.RequestHandler#handleRequest(Object, Context)}
 * or {@link com.amazonaws.services.lambda.runtime.RequestStreamHandler#handleRequest(InputStream, OutputStream, Context)} methods.</p>
 *
 * <p>Using the Java language, {@link com.amazonaws.services.lambda.runtime.RequestHandler} input and output are already
 * strongly typed, and if a json event cannot be deserialize to the specified object,
 * invocation will either fail or retrieve a partial event.
 * More information <a href="https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html">in the documentation (java-handler)</a>.</p>
 *
 * <p>But when using built-in types from the
 * <a href="https://github.com/aws/aws-lambda-java-libs/tree/master/aws-lambda-java-events">aws-lambda-java-events library</a>,
 * such as {@link com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent}
 * or {@link com.amazonaws.services.lambda.runtime.events.SQSEvent},
 * using the {@link Validation} annotation will permit to validate the underlying content,
 * for example the body of an API Gateway request, or the records body of an SQS event.</p>
 *
 * <p>{@link Validation} has built-in validation for the following input types:
 * <table>
 *     <thead><tr><td>Type of event</td><td>Class</td><td>Path to content</td></tr></thead>
 *     <tbody>
 *          <tr><td>API Gateway REST</td><td>{@link com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent}</td><td>{@code body}</td></tr>
 *          <tr><td>API Gateway HTTP</td><td>{@link com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent}</td><td>{@code body}</td></tr>
 *          <tr><td>Cloudformation Custom Resource</td><td>{@link com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent}</td><td>{@code resourceProperties}</td></tr>
 *          <tr><td>CloudWatch Logs</td><td>{@link com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent}</td><td>{@code awslogs.powertools_base64_gzip(data)}</td></tr>
 *          <tr><td>EventBridge / Cloudwatch</td><td>{@link com.amazonaws.services.lambda.runtime.events.ScheduledEvent}</td><td>{@code detail}</td></tr>
 *          <tr><td>Kafka</td><td>{@link com.amazonaws.services.lambda.runtime.events.KafkaEvent}</td><td>{@code records[*][*].value}</td></tr>
 *          <tr><td>Kinesis</td><td>{@link com.amazonaws.services.lambda.runtime.events.KinesisEvent}</td><td>{@code Records[*].kinesis.powertools_base64(data)}</td></tr>
 *          <tr><td>Kinesis Firehose</td><td>{@link com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent}</td><td>{@code Records[*].powertools_base64(data)}</td></tr>
 *          <tr><td>Kinesis Analytics from Firehose</td><td>{@link com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsFirehoseInputPreprocessingEvent}</td><td>{@code Records[*].powertools_base64(data)}</td></tr>
 *          <tr><td>Kinesis Analytics from Streams</td><td>{@link com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsStreamsInputPreprocessingEvent}</td><td>{@code Records[*].powertools_base64(data)}</td></tr>
 *          <tr><td>Load Balancer</td><td>{@link com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent}</td><td>{@code body}</td></tr>
 *          <tr><td>SNS</td><td>{@link com.amazonaws.services.lambda.runtime.events.SNSEvent}</td><td>{@code Records[*].Sns.Message}</td></tr>
 *          <tr><td>SQS</td><td>{@link com.amazonaws.services.lambda.runtime.events.SQSEvent}</td><td>{@code Records[*].body}</td></tr>
 *     </tbody>
 * </table>
 * </p>
 *
 * <p>{@link Validation} has built-in validation for the following output types:
 * <table>
 *     <thead><tr><td>Type of response</td><td>Class</td><td>Path to content</td></tr></thead>
 *     <tbody>
 *          <tr><td>API Gateway REST</td><td>{@link com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent}</td><td>{@code body}</td></tr>
 *          <tr><td>API Gateway HTTP</td><td>{@link com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse}</td><td>{@code body}</td></tr>
 *          <tr><td>API Gateway WebSocket</td><td>{@link com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketResponse}</td><td>{@code body}</td></tr>
 *          <tr><td>Load Balancer</td><td>{@link com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent}</td><td>{@code body}</td></tr>
 *          <tr><td>Kinesis Analytics</td><td>{@link com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsInputPreprocessingResponse}</td><td>{@code Records[*].powertools_base64(data)}</td></tr>
 *     </tbody>
 * </table>
 * </p>
 *
 * <p>
 *     You can specify either inboundSchema or outboundSchema or both, depending on what you want to validate.<br>
 *     The schema must be passed as a json string (constant), or using the syntax {@code "classpath:/some/path/to/schema.json" },
 *     provided that the schema.json file is available in the classpath at the specified path.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Validation {
    /**
     * schema used to validate the lambda function input
     */
    String inboundSchema() default "";

    /**
     * schema used to validate the lambda function output
     */
    String outboundSchema() default "";

    /**
     * path to the sub element
     */
    String envelope() default "";

    /**
     * json schema specification version (default is 2019-09)
     */
    VersionFlag schemaVersion() default V7;
}
