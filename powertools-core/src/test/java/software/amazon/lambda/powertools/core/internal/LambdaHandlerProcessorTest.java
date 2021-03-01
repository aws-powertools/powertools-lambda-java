package software.amazon.lambda.powertools.core.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LambdaHandlerProcessorTest {

    @Test
    void shouldTreatProfilerHandlerMethodAsValid() {
        ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("requestHandler");
        when(pjpMock.getSignature()).thenReturn(signature);

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock))
                .isTrue();
    }

    @Test
    void shouldTreatDefaultHandlerMethodAsValid() {
        ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("handleRequest");
        when(pjpMock.getSignature()).thenReturn(signature);

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock))
                .isTrue();
    }

    @Test
    void shouldNotTreatOtherMethodNamesAsValidHandlerMethod() {
        ProceedingJoinPoint pjpMock = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("handleRequestInvalid");
        when(pjpMock.getSignature()).thenReturn(signature);

        assertThat(LambdaHandlerProcessor.isHandlerMethod(pjpMock))
                .isFalse();
    }
}