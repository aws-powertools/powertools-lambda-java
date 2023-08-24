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

package software.amazon.lambda.powertools.validation.internal;

import static com.networknt.schema.SpecVersion.VersionFlag.V201909;
import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.utilities.jmespath.Base64Function.decode;
import static software.amazon.lambda.powertools.utilities.jmespath.Base64GZipFunction.decompress;
import static software.amazon.lambda.powertools.validation.ValidationUtils.getJsonSchema;
import static software.amazon.lambda.powertools.validation.ValidationUtils.validate;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketResponse;
import com.amazonaws.services.lambda.runtime.events.ActiveMQEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsFirehoseInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsInputPreprocessingResponse;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsStreamsInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.RabbitMQEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.networknt.schema.JsonSchema;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.validation.Validation;
import software.amazon.lambda.powertools.validation.ValidationConfig;

/**
 * Aspect for {@link Validation} annotation
 */
@Aspect
public class ValidationAspect {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationAspect.class);

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(validation)")
    public void callAt(Validation validation) {
    }

    @Around(value = "callAt(validation) && execution(@Validation * *.*(..))", argNames = "pjp,validation")
    public Object around(ProceedingJoinPoint pjp,
                         Validation validation) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();
        boolean validationNeeded = false;

        if (validation.schemaVersion() != V201909) {
            ValidationConfig.get().setSchemaVersion(validation.schemaVersion());
        }

        if (placedOnRequestHandler(pjp)) {
            validationNeeded = true;

            if (!validation.inboundSchema().isEmpty()) {
                JsonSchema inboundJsonSchema = getJsonSchema(validation.inboundSchema(), true);

                Object obj = pjp.getArgs()[0];
                if (validation.envelope() != null && !validation.envelope().isEmpty()) {
                    validate(obj, inboundJsonSchema, validation.envelope());
                } else if (obj instanceof APIGatewayProxyRequestEvent) {
                    APIGatewayProxyRequestEvent event = (APIGatewayProxyRequestEvent) obj;
                    validate(event.getBody(), inboundJsonSchema);
                } else if (obj instanceof APIGatewayV2HTTPEvent) {
                    APIGatewayV2HTTPEvent event = (APIGatewayV2HTTPEvent) obj;
                    validate(event.getBody(), inboundJsonSchema);
                } else if (obj instanceof SNSEvent) {
                    SNSEvent event = (SNSEvent) obj;
                    event.getRecords().forEach(record -> validate(record.getSNS().getMessage(), inboundJsonSchema));
                } else if (obj instanceof SQSEvent) {
                    SQSEvent event = (SQSEvent) obj;
                    event.getRecords().forEach(record -> validate(record.getBody(), inboundJsonSchema));
                } else if (obj instanceof ScheduledEvent) {
                    ScheduledEvent event = (ScheduledEvent) obj;
                    validate(event.getDetail(), inboundJsonSchema);
                } else if (obj instanceof ApplicationLoadBalancerRequestEvent) {
                    ApplicationLoadBalancerRequestEvent event = (ApplicationLoadBalancerRequestEvent) obj;
                    validate(event.getBody(), inboundJsonSchema);
                } else if (obj instanceof CloudWatchLogsEvent) {
                    CloudWatchLogsEvent event = (CloudWatchLogsEvent) obj;
                    validate(decompress(decode(event.getAwsLogs().getData().getBytes(UTF_8))), inboundJsonSchema);
                } else if (obj instanceof CloudFormationCustomResourceEvent) {
                    CloudFormationCustomResourceEvent event = (CloudFormationCustomResourceEvent) obj;
                    validate(event.getResourceProperties(), inboundJsonSchema);
                } else if (obj instanceof KinesisEvent) {
                    KinesisEvent event = (KinesisEvent) obj;
                    event.getRecords()
                            .forEach(record -> validate(decode(record.getKinesis().getData()), inboundJsonSchema));
                } else if (obj instanceof KinesisFirehoseEvent) {
                    KinesisFirehoseEvent event = (KinesisFirehoseEvent) obj;
                    event.getRecords().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else if (obj instanceof KafkaEvent) {
                    KafkaEvent event = (KafkaEvent) obj;
                    event.getRecords().forEach((s, records) -> records.forEach(
                            record -> validate(decode(record.getValue()), inboundJsonSchema)));
                } else if (obj instanceof ActiveMQEvent) {
                    ActiveMQEvent event = (ActiveMQEvent) obj;
                    event.getMessages().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else if (obj instanceof RabbitMQEvent) {
                    RabbitMQEvent event = (RabbitMQEvent) obj;
                    event.getRmqMessagesByQueue().forEach((s, records) -> records.forEach(
                            record -> validate(decode(record.getData()), inboundJsonSchema)));
                } else if (obj instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
                    KinesisAnalyticsFirehoseInputPreprocessingEvent event =
                            (KinesisAnalyticsFirehoseInputPreprocessingEvent) obj;
                    event.getRecords().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else if (obj instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
                    KinesisAnalyticsStreamsInputPreprocessingEvent event =
                            (KinesisAnalyticsStreamsInputPreprocessingEvent) obj;
                    event.getRecords().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else {
                    LOG.warn("Unhandled event type {}, please use the 'envelope' parameter to specify what to validate",
                            obj.getClass().getName());
                }
            }
        }

        Object result = pjp.proceed(proceedArgs);

        if (validationNeeded && !validation.outboundSchema().isEmpty()) {
            JsonSchema outboundJsonSchema = getJsonSchema(validation.outboundSchema(), true);

            if (result instanceof APIGatewayProxyResponseEvent) {
                APIGatewayProxyResponseEvent response = (APIGatewayProxyResponseEvent) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof APIGatewayV2HTTPResponse) {
                APIGatewayV2HTTPResponse response = (APIGatewayV2HTTPResponse) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof APIGatewayV2WebSocketResponse) {
                APIGatewayV2WebSocketResponse response = (APIGatewayV2WebSocketResponse) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof ApplicationLoadBalancerResponseEvent) {
                ApplicationLoadBalancerResponseEvent response = (ApplicationLoadBalancerResponseEvent) result;
                validate(response.getBody(), outboundJsonSchema);
            } else if (result instanceof KinesisAnalyticsInputPreprocessingResponse) {
                KinesisAnalyticsInputPreprocessingResponse response =
                        (KinesisAnalyticsInputPreprocessingResponse) result;
                response.getRecords().forEach(record -> validate(decode(record.getData()), outboundJsonSchema));
            } else {
                LOG.warn("Unhandled response type {}, please use the 'envelope' parameter to specify what to validate",
                        result.getClass().getName());
            }
        }

        return result;
    }
}
