package software.amazon.lambda.powertools.parameters.transform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Base64TransformerTest {

    @Test
    public void transform_base64_shouldTransformInString() {
        Base64Transformer transformer = new Base64Transformer();

        String s = transformer.applyTransformation(Base64.getEncoder().encodeToString("foobar".getBytes()));

        assertEquals("foobar", s);
    }

    @Test
    public void transform_base64WrongFormat_shouldThrowException() {
        Base64Transformer transformer = new Base64Transformer();

        assertThrows(TransformationException.class, () -> transformer.applyTransformation("foobarbaz"));
    }
}
