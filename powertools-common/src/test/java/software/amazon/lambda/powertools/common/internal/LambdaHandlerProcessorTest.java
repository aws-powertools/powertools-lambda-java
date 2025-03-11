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

package software.amazon.lambda.powertools.common.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class LambdaHandlerProcessorTest {

    private Signature signature = mock(Signature.class);
    private ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);

    @Test
    void isHandlerMethod_shouldRecognizeRequestHandler() {
        Object[] args = {new Object(), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestHandler.class, args);

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock)).isTrue();
    }

    @Test
    void isHandlerMethod_shouldRecognizeRequestStreamHandler() {
        Object[] args = {mock(InputStream.class), mock(OutputStream.class), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock)).isTrue();
    }

    @Test
    void isHandlerMethod_shouldReturnFalse() {
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(Object.class, new Object[] {});

        boolean isHandlerMethod = LambdaHandlerProcessor.isHandlerMethod(pjpMock);

        assertThat(isHandlerMethod).isFalse();
    }

    @Test
    void placedOnRequestHandler_shouldRecognizeRequestHandler() {
        Object[] args = {new Object(), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestHandler.class, args);

        assertThat(LambdaHandlerProcessor.placedOnRequestHandler(pjpMock)).isTrue();
    }

    @Test
    void placedOnStreamHandler_shouldRecognizeRequestStreamHandler() {
        Object[] args = {mock(InputStream.class), mock(OutputStream.class), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        assertThat(LambdaHandlerProcessor.placedOnStreamHandler(pjpMock)).isTrue();
    }

    @Test
    void placedOnRequestHandler_shouldInvalidateOnWrongNoOfArgs() {
        Object[] args = {new Object()};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestHandler.class, args);

        boolean isPlacedOnRequestHandler = LambdaHandlerProcessor.placedOnRequestHandler(pjpMock);

        assertThat(isPlacedOnRequestHandler).isFalse();
    }

    @Test
    void placedOnRequestHandler_shouldInvalidateOnWrongTypeOfArgs() {
        Object[] args = {new Object(), new Object()};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestHandler.class, args);

        boolean isPlacedOnRequestHandler = LambdaHandlerProcessor.placedOnRequestHandler(pjpMock);

        assertThat(isPlacedOnRequestHandler).isFalse();
    }

    @Test
    void placedOnStreamHandler_shouldInvalidateOnWrongNoOfArgs() {
        Object[] args = {new Object()};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        boolean isPlacedOnStreamHandler = LambdaHandlerProcessor.placedOnStreamHandler(pjpMock);

        assertThat(isPlacedOnStreamHandler).isFalse();
    }

    @Test
    void placedOnStreamHandler_shouldInvalidateOnWrongTypeOfArgs() {
        Object[] args = {new Object(), new Object(), new Object()};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        boolean isPlacedOnStreamHandler = LambdaHandlerProcessor.placedOnStreamHandler(pjpMock);

        assertThat(isPlacedOnStreamHandler).isFalse();
    }

    @Test
    void placedOnStreamHandler_shouldInvalidateOnTypeOfArgs_invalidOutputStreamArg() {
        Object[] args = {mock(InputStream.class), new Object(), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        boolean isPlacedOnStreamHandler = LambdaHandlerProcessor.placedOnStreamHandler(pjpMock);

        assertThat(isPlacedOnStreamHandler).isFalse();
    }

    @Test
    void placedOnStreamHandler_shouldInvalidateOnTypeOfArgs_invalidContextArg() {
        Object[] args = {mock(InputStream.class), mock(OutputStream.class), new Object()};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        boolean isPlacedOnStreamHandler = LambdaHandlerProcessor.placedOnStreamHandler(pjpMock);

        assertThat(isPlacedOnStreamHandler).isFalse();
    }

    @Test
    @SetEnvironmentVariable(key = LambdaConstants.X_AMZN_TRACE_ID, value = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1\"")
    void getXrayTraceId_present() {
        String traceID = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1\"";

        Optional xRayTraceId = LambdaHandlerProcessor.getXrayTraceId();

        assertThat(xRayTraceId.isPresent()).isTrue();
        assertThat(traceID.split(";")[0].replace(LambdaConstants.ROOT_EQUALS, "")).isEqualTo(xRayTraceId.get());
    }

    @Test
    @ClearEnvironmentVariable(key = LambdaConstants.X_AMZN_TRACE_ID)
    void getXrayTraceId_notPresent() {

        boolean isXRayTraceIdPresent = LambdaHandlerProcessor.getXrayTraceId().isPresent();

        assertThat(isXRayTraceIdPresent).isFalse();
    }

    @Test
    void extractContext_fromRequestHandler() {
        Object[] args = {new Object(), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestHandler.class, args);

        Context context = LambdaHandlerProcessor.extractContext(pjpMock);

        assertThat(context).isNotNull();
    }

    @Test
    void extractContext_fromStreamRequestHandler() {
        Object[] args = {mock(InputStream.class), mock(OutputStream.class), mock(Context.class)};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(RequestStreamHandler.class, args);

        Context context = LambdaHandlerProcessor.extractContext(pjpMock);

        assertNotNull(context);
    }

    @Test
    void extractContext_notKnownHandler() {
        Object[] args = {new Object()};
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp(Object.class, args);

        Context context = LambdaHandlerProcessor.extractContext(pjpMock);

        assertThat(context).isNull();
    }

    @Test
    void isColdStart() {
        boolean isColdStart = LambdaHandlerProcessor.isColdStart();

        assertThat(isColdStart).isTrue();
    }

    @Test
    void isColdStart_coldStartDone() {
        LambdaHandlerProcessor.coldStartDone();

        boolean isColdStart = LambdaHandlerProcessor.isColdStart();

        assertThat(isColdStart).isFalse();
    }

    @Test
    @SetEnvironmentVariable(key = LambdaConstants.AWS_SAM_LOCAL, value = "true")
    void isSamLocal() {

        boolean isSamLocal = LambdaHandlerProcessor.isSamLocal();

        assertThat(isSamLocal).isTrue();
    }

    @Test
    @SetEnvironmentVariable(key = LambdaConstants.POWERTOOLS_SERVICE_NAME, value = "MyService")
    void serviceName() {
        String expectedServiceName = "MyService";
        String actualServiceName = LambdaHandlerProcessor.serviceName();

        assertThat(actualServiceName).isEqualTo(expectedServiceName);
    }

    @Test
    @ClearEnvironmentVariable(key = LambdaConstants.POWERTOOLS_SERVICE_NAME)
    void serviceName_Undefined() {
        LambdaHandlerProcessor.resetServiceName();
        assertThat(LambdaHandlerProcessor.serviceName()).isEqualTo(LambdaConstants.SERVICE_UNDEFINED);
    }

    private ProceedingJoinPoint mockRequestHandlerPjp(Class handlerClass, Object[] handlerArgs) {
        when(signature.getDeclaringType()).thenReturn(handlerClass);
        when(pjpMock.getArgs()).thenReturn(handlerArgs);
        when(pjpMock.getSignature()).thenReturn(signature);
        return pjpMock;
    }
}