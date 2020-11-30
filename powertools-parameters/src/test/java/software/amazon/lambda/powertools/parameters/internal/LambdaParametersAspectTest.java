package software.amazon.lambda.powertools.parameters.internal;

import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.parameters.Param;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.transform.Base64Transformer;
import software.amazon.lambda.powertools.parameters.transform.JsonTransformer;
import software.amazon.lambda.powertools.parameters.transform.ObjectToDeserialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LambdaParametersAspectTest {

    @Param(key = "/simple", provider = CustomProvider.class)
    private String param;

    @Param(key = "/base64", provider = CustomProvider.class, transformer = Base64Transformer.class)
    private String basicTransform;

    @Param(key = "/json", provider = CustomProvider.class, transformer = JsonTransformer.class)
    private ObjectToDeserialize complexTransform;

    @Param(key = "/json", provider = CustomProvider.class, transformer = JsonTransformer.class)
    private AnotherObject wrongTransform;

    @Test
    public void testSimple() {
        String paramValue = param;

        assertThat(paramValue).isEqualTo("value");
    }

    @Test
    public void testWithBasicTransform() {
        String paramValue = basicTransform;
        assertThat(paramValue).isEqualTo("value");
    }

    @Test
    public void testWithComplexTransform() {
        ObjectToDeserialize paramValue = complexTransform;
        assertThat(paramValue).isNotNull();
        assertThat(paramValue).isInstanceOf(ObjectToDeserialize.class);
        assertThat(paramValue).matches(
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
