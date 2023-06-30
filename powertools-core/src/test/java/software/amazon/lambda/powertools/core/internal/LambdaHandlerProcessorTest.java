package software.amazon.lambda.powertools.core.internal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LambdaHandlerProcessorTest {

    @Test
    void isHandlerMethod_shouldRecognizeRequestHandler() {
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp();

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock)).isTrue();
    }

    @Test
    void isHandlerMethod_shouldRecognizeRequestStreamHandler() {
        ProceedingJoinPoint pjpMock = mockRequestStreamHandlerPjp();

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock)).isTrue();
    }

    @Test
    void placedOnRequestHandler_shouldRecognizeRequestHandler() {
        ProceedingJoinPoint pjpMock = mockRequestHandlerPjp();

        assertThat(LambdaHandlerProcessor.placedOnRequestHandler(pjpMock)).isTrue();
    }

    @Test
    void placedOnStreamHandler_shouldRecognizeRequestStreamHandler() {
        ProceedingJoinPoint pjpMock = mockRequestStreamHandlerPjp();

        assertThat(LambdaHandlerProcessor.placedOnStreamHandler(pjpMock)).isTrue();
    }

    private static ProceedingJoinPoint mockRequestHandlerPjp() {
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringType()).thenReturn(RequestHandler.class);
        ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
        Object[] args = {new Object(), mock(Context.class)};
        when(pjpMock.getArgs()).thenReturn(args);
        when(pjpMock.getSignature()).thenReturn(signature);
        return pjpMock;
    }

    private static ProceedingJoinPoint mockRequestStreamHandlerPjp() {
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringType()).thenReturn(RequestStreamHandler.class);
        ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
        Object[] args = {mock(InputStream.class), mock(OutputStream.class), mock(Context.class)};
        when(pjpMock.getArgs()).thenReturn(args);
        when(pjpMock.getSignature()).thenReturn(signature);
        return pjpMock;
    }
}