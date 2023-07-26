package software.amazon.lambda.powertools.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static software.amazon.lambda.powertools.core.internal.SystemWrapper.getenv;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
    void getXrayTraceId_present() {
        String traceID = "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1\"";
        try (MockedStatic<SystemWrapper> mockedSystemWrapper = mockStatic(SystemWrapper.class)) {
            mockedSystemWrapper.when(() -> getenv(LambdaConstants.X_AMZN_TRACE_ID)).thenReturn(traceID);

            Optional xRayTraceId = LambdaHandlerProcessor.getXrayTraceId();

            assertThat(xRayTraceId.isPresent()).isTrue();
            assertThat(traceID.split(";")[0].replace(LambdaConstants.ROOT_EQUALS, "")).isEqualTo(xRayTraceId.get());
        }
    }

    @Test
    void getXrayTraceId_notPresent() {
        try (MockedStatic<SystemWrapper> mockedSystemWrapper = mockStatic(SystemWrapper.class)) {
            mockedSystemWrapper.when(() -> getenv(LambdaConstants.X_AMZN_TRACE_ID)).thenReturn(null);

            boolean isXRayTraceIdPresent = LambdaHandlerProcessor.getXrayTraceId().isPresent();

            assertThat(isXRayTraceIdPresent).isFalse();
        }
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
    void isSamLocal() {
        try (MockedStatic<SystemWrapper> mockedSystemWrapper = mockStatic(SystemWrapper.class)) {
            mockedSystemWrapper.when(() -> getenv(LambdaConstants.AWS_SAM_LOCAL)).thenReturn("true");

            boolean isSamLocal = LambdaHandlerProcessor.isSamLocal();

            assertThat(isSamLocal).isTrue();
        }
    }

    @Test
    void serviceName() {
        try (MockedStatic<SystemWrapper> mockedSystemWrapper = mockStatic(SystemWrapper.class)) {
            String expectedServiceName = "MyService";
            mockedSystemWrapper.when(() -> getenv(LambdaConstants.POWERTOOLS_SERVICE_NAME))
                    .thenReturn(expectedServiceName);

            String actualServiceName = LambdaHandlerProcessor.serviceName();

            assertThat(actualServiceName).isEqualTo(expectedServiceName);
        }
    }

    @Test
    void serviceName_Undefined() {
        LambdaHandlerProcessor.resetServiceName();
        try (MockedStatic<SystemWrapper> mockedSystemWrapper = mockStatic(SystemWrapper.class)) {
            mockedSystemWrapper.when(() -> getenv(LambdaConstants.POWERTOOLS_SERVICE_NAME)).thenReturn(null);

            assertThat(LambdaHandlerProcessor.serviceName()).isEqualTo(LambdaConstants.SERVICE_UNDEFINED);
        }
    }

    private ProceedingJoinPoint mockRequestHandlerPjp(Class handlerClass, Object[] handlerArgs) {
        when(signature.getDeclaringType()).thenReturn(handlerClass);
        when(pjpMock.getArgs()).thenReturn(handlerArgs);
        when(pjpMock.getSignature()).thenReturn(signature);
        return pjpMock;
    }
}