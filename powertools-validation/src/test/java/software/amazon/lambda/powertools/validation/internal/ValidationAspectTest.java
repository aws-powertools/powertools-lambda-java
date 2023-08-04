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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.ActiveMQEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsFirehoseInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsStreamsInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.RabbitMQEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.util.stream.Stream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.lambda.powertools.validation.Validation;
import software.amazon.lambda.powertools.validation.ValidationConfig;
import software.amazon.lambda.powertools.validation.ValidationException;
import software.amazon.lambda.powertools.validation.handlers.GenericSchemaV7Handler;
import software.amazon.lambda.powertools.validation.handlers.SQSWithCustomEnvelopeHandler;
import software.amazon.lambda.powertools.validation.handlers.SQSWithWrongEnvelopeHandler;
import software.amazon.lambda.powertools.validation.handlers.ValidationInboundStringHandler;
import software.amazon.lambda.powertools.validation.model.MyCustomEvent;


public class ValidationAspectTest {

    @Mock
    Validation validation;
    @Mock
    Signature signature;
    @Mock
    private Context context;
    @Mock
    private ProceedingJoinPoint pjp;
    private ValidationAspect validationAspect = new ValidationAspect();

    private static Stream<Arguments> provideEventAndEventType() {
        return Stream.of(
                Arguments.of("/sns_event.json", SNSEvent.class),
                Arguments.of("/scheduled_event.json", ScheduledEvent.class),
                Arguments.of("/alb_event.json", ApplicationLoadBalancerRequestEvent.class),
                Arguments.of("/cwl_event.json", CloudWatchLogsEvent.class),
                Arguments.of("/cfcr_event.json", CloudFormationCustomResourceEvent.class),
                Arguments.of("/kf_event.json", KinesisFirehoseEvent.class),
                Arguments.of("/kafka_event.json", KafkaEvent.class),
                Arguments.of("/amq_event.json", ActiveMQEvent.class),
                Arguments.of("/rabbitmq_event.json", RabbitMQEvent.class),
                Arguments.of("/kafip_event.json", KinesisAnalyticsFirehoseInputPreprocessingEvent.class),
                Arguments.of("/kasip_event.json", KinesisAnalyticsStreamsInputPreprocessingEvent.class),
                Arguments.of("/custom_event.json", MyCustomEvent.class)

        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @ArgumentsSource(ResponseEventsArgumentsProvider.class)
    public void testValidateOutboundJsonSchema(Object object) throws Throwable {
        when(validation.schemaVersion()).thenReturn(SpecVersion.VersionFlag.V7);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getSignature().getDeclaringType()).thenReturn(RequestHandler.class);
        Object[] args = {new Object(), context};
        when(pjp.getArgs()).thenReturn(args);
        when(pjp.proceed(args)).thenReturn(object);
        when(validation.inboundSchema()).thenReturn("");
        when(validation.outboundSchema()).thenReturn("classpath:/schema_v7.json");

        assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
        {
            validationAspect.around(pjp, validation);
        });
    }

    @Test
    public void testValidateOutboundJsonSchema_APIGWV2() throws Throwable {
        when(validation.schemaVersion()).thenReturn(SpecVersion.VersionFlag.V7);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getSignature().getDeclaringType()).thenReturn(RequestHandler.class);
        Object[] args = {new Object(), context};
        when(pjp.getArgs()).thenReturn(args);
        APIGatewayV2HTTPResponse apiGatewayV2HTTPResponse = new APIGatewayV2HTTPResponse();
        apiGatewayV2HTTPResponse.setBody("{" +
                "    \"id\": 1," +
                "    \"name\": \"Lampshade\"," +
                "    \"price\": 42" +
                "}");
        when(pjp.proceed(args)).thenReturn(apiGatewayV2HTTPResponse);
        when(validation.inboundSchema()).thenReturn("");
        when(validation.outboundSchema()).thenReturn("classpath:/schema_v7.json");

        assertThatNoException().isThrownBy(() -> validationAspect.around(pjp, validation));
    }

    @Test
    public void validate_inputOK_schemaInClasspath_shouldValidate() {
        GenericSchemaV7Handler<APIGatewayProxyRequestEvent> handler = new GenericSchemaV7Handler();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{" +
                "    \"id\": 1," +
                "    \"name\": \"Lampshade\"," +
                "    \"price\": 42" +
                "}");
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_inputKO_schemaInClasspath_shouldThrowValidationException() {
        GenericSchemaV7Handler<APIGatewayProxyRequestEvent> handler = new GenericSchemaV7Handler();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody("{" +
                "    \"id\": 1," +
                "    \"name\": \"Lampshade\"," +
                "    \"price\": -2" +
                "}");
        // price is negative
        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> handler.handleRequest(event, context));
    }

    @Test
    public void validate_inputOK_schemaInString_shouldValidate() {
        ValidationInboundStringHandler handler = new ValidationInboundStringHandler();
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody("{" +
                "    \"id\": 1," +
                "    \"name\": \"Lampshade\"," +
                "    \"price\": 42" +
                "}");
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_inputKO_schemaInString_shouldThrowValidationException() {
        ValidationInboundStringHandler handler = new ValidationInboundStringHandler();
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody("{" +
                "    \"id\": 1," +
                "    \"name\": \"Lampshade\"" +
                "}");
        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> handler.handleRequest(event, context));
    }

    @Test
    public void validate_SQS() {
        PojoSerializer<SQSEvent> pojoSerializer =
                LambdaEventSerializers.serializerFor(SQSEvent.class, ClassLoader.getSystemClassLoader());
        SQSEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/sqs.json"));

        GenericSchemaV7Handler handler = new GenericSchemaV7Handler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_SQS_CustomEnvelopeTakePrecedence() {
        PojoSerializer<SQSEvent> pojoSerializer =
                LambdaEventSerializers.serializerFor(SQSEvent.class, ClassLoader.getSystemClassLoader());
        SQSEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/sqs_message.json"));

        SQSWithCustomEnvelopeHandler handler = new SQSWithCustomEnvelopeHandler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_SQS_WrongEnvelope_shouldThrowValidationException() {
        PojoSerializer<SQSEvent> pojoSerializer =
                LambdaEventSerializers.serializerFor(SQSEvent.class, ClassLoader.getSystemClassLoader());
        SQSEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/sqs_message.json"));

        SQSWithWrongEnvelopeHandler handler = new SQSWithWrongEnvelopeHandler();
        assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> handler.handleRequest(event, context));
    }

    @Test
    public void validate_Kinesis() {
        PojoSerializer<KinesisEvent> pojoSerializer =
                LambdaEventSerializers.serializerFor(KinesisEvent.class, ClassLoader.getSystemClassLoader());
        KinesisEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/kinesis.json"));

        GenericSchemaV7Handler handler = new GenericSchemaV7Handler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @ParameterizedTest
    @MethodSource("provideEventAndEventType")
    public void validateEEvent(String jsonResource, Class eventClass) throws IOException {
        Object event = ValidationConfig.get().getObjectMapper()
                .readValue(this.getClass().getResourceAsStream(jsonResource), eventClass);

        GenericSchemaV7Handler<Object> handler = new GenericSchemaV7Handler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }
}
