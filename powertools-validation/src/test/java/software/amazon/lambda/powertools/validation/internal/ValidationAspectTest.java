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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.lambda.powertools.validation.ValidationConfig;
import software.amazon.lambda.powertools.validation.ValidationException;
import software.amazon.lambda.powertools.validation.handlers.*;
import software.amazon.lambda.powertools.validation.model.MyCustomEvent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ValidationAspectTest {

    @Mock
    private Context context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void validate_inputOK_schemaInClasspath_shouldValidate() {
        ValidationInboundClasspathHandler handler = new ValidationInboundClasspathHandler();
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
        ValidationInboundClasspathHandler handler = new ValidationInboundClasspathHandler();
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
        PojoSerializer<SQSEvent> pojoSerializer = LambdaEventSerializers.serializerFor(SQSEvent.class, ClassLoader.getSystemClassLoader());
        SQSEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/sqs.json"));

        SQSHandler handler = new SQSHandler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_SQS_CustomEnvelopeTakePrecedence() {
        PojoSerializer<SQSEvent> pojoSerializer = LambdaEventSerializers.serializerFor(SQSEvent.class, ClassLoader.getSystemClassLoader());
        SQSEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/sqs_message.json"));

        SQSWithCustomEnvelopeHandler handler = new SQSWithCustomEnvelopeHandler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_Kinesis() {
        PojoSerializer<KinesisEvent> pojoSerializer = LambdaEventSerializers.serializerFor(KinesisEvent.class, ClassLoader.getSystemClassLoader());
        KinesisEvent event = pojoSerializer.fromJson(this.getClass().getResourceAsStream("/kinesis.json"));

        KinesisHandler handler = new KinesisHandler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }

    @Test
    public void validate_CustomObject() throws IOException {
        MyCustomEvent event = ValidationConfig.get().getObjectMapper().readValue(this.getClass().getResourceAsStream("/custom_event.json"), MyCustomEvent.class);

        MyCustomEventHandler handler = new MyCustomEventHandler();
        assertThat(handler.handleRequest(event, context)).isEqualTo("OK");
    }
}
