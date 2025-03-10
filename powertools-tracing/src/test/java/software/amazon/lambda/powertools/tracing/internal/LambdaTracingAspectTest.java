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

package software.amazon.lambda.powertools.tracing.internal;

import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.xray.AWSXRay;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.tracing.handlers.PowerToolDisabled;
import software.amazon.lambda.powertools.tracing.handlers.PowerToolDisabledForStream;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabled;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledExplicitlyForResponseAndError;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledForError;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledForResponse;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledForResponseWithCustomMapper;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledForStream;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledForStreamWithNoMetaData;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledWithException;
import software.amazon.lambda.powertools.tracing.handlers.PowerTracerToolEnabledWithNoMetaData;
import software.amazon.lambda.powertools.tracing.nonhandler.PowerToolNonHandler;


@SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_RESPONSE", value = "false")
@SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_ERROR", value = "false")
class LambdaTracingAspectTest {
    private RequestHandler<Object, Object> requestHandler;
    private RequestStreamHandler streamHandler;
    private PowerToolNonHandler nonHandlerMethod;

    @Mock
    private Context context;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        openMocks(this);
        writeStaticField(LambdaHandlerProcessor.class, "IS_COLD_START", null, true);
        setupContext();
        requestHandler = new PowerTracerToolEnabled();
        streamHandler = new PowerTracerToolEnabledForStream();
        nonHandlerMethod = new PowerToolNonHandler();
        AWSXRay.beginSegment(LambdaTracingAspectTest.class.getName());
    }

    @AfterEach
    void tearDown() {
        AWSXRay.endSegment();
    }

    @Test
    void shouldCaptureNonHandlerMethod() {
        nonHandlerMethod.doSomething();

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .anySatisfy(segment ->
                        assertThat(segment.getName()).isEqualTo("## doSomething"));
    }

    @Test
    void shouldCaptureNonHandlerMethodWithCustomSegmentName() {
        nonHandlerMethod.doSomethingCustomName();

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .anySatisfy(segment ->
                        assertThat(segment.getName()).isEqualTo("custom"));
    }

    @Test
    void shouldCaptureTraces() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(0);
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_RESPONSE", value = "true")
    void shouldCaptureTracesWithResponseMetadata() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("lambdaHandler");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_ERROR", value = "true")
    void shouldCaptureTracesWithExceptionMetaData() {
        requestHandler = new PowerTracerToolEnabledWithException();

        Throwable exception = catchThrowable(() -> requestHandler.handleRequest(new Object(), context));

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("lambdaHandler");

                    assertThat(subsegment.getMetadata().get("lambdaHandler"))
                            .satisfies(stringObjectMap -> assertThat(stringObjectMap)
                                    .containsEntry("handleRequest error", exception));
                });
    }

    @Test
    void shouldCaptureTracesForStream() throws IOException {
        streamHandler.handleRequest(new ByteArrayInputStream("test".getBytes()), new ByteArrayOutputStream(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "streamHandler");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_RESPONSE", value = "true")
    void shouldCaptureTracesForStreamWithResponseMetadata() throws IOException {
        streamHandler.handleRequest(new ByteArrayInputStream("test".getBytes()), new ByteArrayOutputStream(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "streamHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("streamHandler");
                });
    }

    @Test
    void shouldNotCaptureTracesNotEnabled() throws IOException {
        requestHandler = new PowerToolDisabled();
        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .isEmpty();

        streamHandler = new PowerToolDisabledForStream();
        streamHandler.handleRequest(new ByteArrayInputStream("test".getBytes()), new ByteArrayOutputStream(), context);

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .isEmpty();
    }

    @Test
    void shouldCaptureTracesWithNoMetadata() {
        requestHandler = new PowerTracerToolEnabledWithNoMetaData();

        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "service_undefined");

                    assertThat(subsegment.getMetadata())
                            .isEmpty();
                });
    }

    @Test
    void shouldCaptureTracesForStreamWithNoMetadata() throws IOException {
        streamHandler = new PowerTracerToolEnabledForStreamWithNoMetaData();

        streamHandler.handleRequest(new ByteArrayInputStream("test".getBytes()), new ByteArrayOutputStream(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "service_undefined");

                    assertThat(subsegment.getMetadata())
                            .isEmpty();
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_RESPONSE", value = "false")
    void shouldNotCaptureTracesIfDisabledViaEnvironmentVariable() {
        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .isEmpty();
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_RESPONSE", value = "false")
    void shouldCaptureTracesIfExplicitlyEnabledAndEnvironmentVariableIsDisabled() {
        requestHandler = new PowerTracerToolEnabledForResponse();

        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("lambdaHandler");
                });
    }

    @Test
    void shouldCaptureTracesForSelfReferencingReturnTypesViaCustomMapper() {
        requestHandler = new PowerTracerToolEnabledForResponseWithCustomMapper();

        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("lambdaHandler");

                    assertThat(subsegment.getMetadata().get("lambdaHandler"))
                            .hasFieldOrPropertyWithValue("handleRequest response",
                                    "{\"name\":\"parent\",\"c\":{\"name\":\"child\",\"p\":\"parent\"}}");
                });

        assertThatNoException().isThrownBy(AWSXRay::endSegment);

        AWSXRay.beginSegment(LambdaTracingAspectTest.class.getName());
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_RESPONSE", value = "false")
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_ERROR", value = "false")
    void shouldCaptureTracesIfExplicitlyEnabledBothAndEnvironmentVariableIsDisabled() {
        requestHandler = new PowerTracerToolEnabledExplicitlyForResponseAndError();

        requestHandler.handleRequest(new Object(), context);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("lambdaHandler");
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_ERROR", value = "false")
    void shouldNotCaptureTracesWithExceptionMetaDataIfDisabledViaEnvironmentVariable() {
        requestHandler = new PowerTracerToolEnabledWithException();

        Throwable throwable = catchThrowable(() -> requestHandler.handleRequest(new Object(), context));

        assertThat(throwable)
                .isInstanceOf(RuntimeException.class);

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .isEmpty();
                });
    }

    @Test
    @SetEnvironmentVariable(key = "POWERTOOLS_TRACER_CAPTURE_ERROR", value = "false")
    void shouldCaptureTracesWithExceptionMetaDataEnabledExplicitlyAndEnvironmentVariableDisabled() {
        requestHandler = new PowerTracerToolEnabledForError();

        Throwable exception = catchThrowable(() -> requestHandler.handleRequest(new Object(), context));

        assertThat(AWSXRay.getTraceEntity())
                .isNotNull();

        assertThat(AWSXRay.getTraceEntity().getSubsegmentsCopy())
                .hasSize(1)
                .allSatisfy(subsegment ->
                {
                    assertThat(subsegment.getAnnotations())
                            .hasSize(2)
                            .containsEntry("ColdStart", true)
                            .containsEntry("Service", "lambdaHandler");

                    assertThat(subsegment.getMetadata())
                            .hasSize(1)
                            .containsKey("lambdaHandler");

                    assertThat(subsegment.getMetadata().get("lambdaHandler"))
                            .satisfies(stringObjectMap -> assertThat(stringObjectMap)
                                    .containsEntry("handleRequest error", exception));
                });
    }

    private void setupContext() {
        when(context.getFunctionName()).thenReturn("testFunction");
        when(context.getInvokedFunctionArn()).thenReturn("testArn");
        when(context.getFunctionVersion()).thenReturn("1");
        when(context.getMemoryLimitInMB()).thenReturn(10);
    }
}
