package software.amazon.lambda.powertools.parameters.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import software.amazon.lambda.powertools.parameters.Param;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.transform.Base64Transformer;
import software.amazon.lambda.powertools.parameters.transform.JsonTransformer;
import software.amazon.lambda.powertools.parameters.transform.ObjectToDeserialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class LambdaParametersAspectTest {

    @Mock
    private SSMProvider defaultProvider;

    @Param(key = "/default")
    private String defaultValue;

    @Param(key = "/simple", provider = CustomProvider.class)
    private String param;

    @Param(key = "/base64", provider = CustomProvider.class, transformer = Base64Transformer.class)
    private String basicTransform;

    @Param(key = "/json", provider = CustomProvider.class, transformer = JsonTransformer.class)
    private ObjectToDeserialize complexTransform;

    @Param(key = "/json", provider = CustomProvider.class, transformer = JsonTransformer.class)
    private AnotherObject wrongTransform;

    @BeforeEach
    public void init() {
        openMocks(this);
    }

    @Test
    public void testDefault_ShouldUseSSMProvider() {
        try (MockedStatic<ParamManager> mocked = mockStatic(ParamManager.class)) {
            mocked.when(() -> ParamManager.getProvider(SSMProvider.class)).thenReturn(defaultProvider);
            when(defaultProvider.get("/default")).thenReturn("value");

            assertThat(defaultValue).isEqualTo("value");
            mocked.verify(() -> ParamManager.getProvider(SSMProvider.class), times(1));
            verify(defaultProvider, times(1)).get("/default");

            mocked.reset();
        }
    }

    @Test
    public void testSimple() {
        assertThat(param).isEqualTo("value");
    }

    @Test
    public void testWithBasicTransform() {
        assertThat(basicTransform).isEqualTo("value");
    }

    @Test
    public void testWithComplexTransform() {
        assertThat(complexTransform)
                .isInstanceOf(ObjectToDeserialize.class)
                .matches(
                        o -> o.getFoo().equals("Foo") &&
                        o.getBar() == 42 &&
                        o.getBaz() == 123456789);
    }

    @Test
    public void testWithComplexTransformWrongTargetClass_ShouldThrowException() {
        assertThatExceptionOfType(TransformationException.class)
                .isThrownBy(() -> {AnotherObject obj = wrongTransform; });
    }

}
