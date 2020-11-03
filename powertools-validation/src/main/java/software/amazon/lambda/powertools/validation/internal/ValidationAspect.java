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
package software.amazon.lambda.powertools.validation.internal;

import com.amazonaws.services.lambda.runtime.events.*;
import com.networknt.schema.JsonSchema;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.validation.Validation;
import software.amazon.lambda.powertools.validation.ValidationConfig;

import static com.networknt.schema.SpecVersion.VersionFlag.V201909;
import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.validation.ValidationUtils.getJsonSchema;
import static software.amazon.lambda.powertools.validation.ValidationUtils.validate;
import static software.amazon.lambda.powertools.validation.jmespath.Base64Function.decode;
import static software.amazon.lambda.powertools.validation.jmespath.Base64GZipFunction.decompress;

/**
 * Aspect for {@link Validation} annotation
 */
@Aspect
public class ValidationAspect {
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

        if (isHandlerMethod(pjp)
                && placedOnRequestHandler(pjp)) {
            validationNeeded = true;

            if (!validation.inboundSchema().isEmpty()) {
                JsonSchema inboundJsonSchema = getJsonSchema(validation.inboundSchema(), true);

                Object obj = pjp.getArgs()[0];
                if (obj instanceof APIGatewayProxyRequestEvent) {
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
                    event.getRecords().forEach(record -> validate(decode(record.getKinesis().getData()), inboundJsonSchema));
                } else if (obj instanceof KinesisFirehoseEvent) {
                    KinesisFirehoseEvent event = (KinesisFirehoseEvent) obj;
                    event.getRecords().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else if (obj instanceof KafkaEvent) {
                    KafkaEvent event = (KafkaEvent) obj;
                    event.getRecords().forEach((s, records) -> records.forEach(record -> validate(record.getValue(), inboundJsonSchema)));
                }else if (obj instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
                    KinesisAnalyticsFirehoseInputPreprocessingEvent event = (KinesisAnalyticsFirehoseInputPreprocessingEvent) obj;
                    event.getRecords().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else if (obj instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
                    KinesisAnalyticsStreamsInputPreprocessingEvent event = (KinesisAnalyticsStreamsInputPreprocessingEvent) obj;
                    event.getRecords().forEach(record -> validate(decode(record.getData()), inboundJsonSchema));
                } else {
                    validate(obj, inboundJsonSchema, validation.envelope());
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
                KinesisAnalyticsInputPreprocessingResponse response = (KinesisAnalyticsInputPreprocessingResponse) result;
                response.getRecords().forEach(record -> validate(decode(record.getData()), outboundJsonSchema));
            } else {
                validate(result, outboundJsonSchema, validation.envelope());
            }
        }

        return result;
    }
}
