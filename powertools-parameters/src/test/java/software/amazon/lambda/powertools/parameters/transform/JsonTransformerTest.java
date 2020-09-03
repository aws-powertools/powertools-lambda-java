package software.amazon.lambda.powertools.parameters.transform;

import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonTransformerTest {

    @Test
    public void transform_json_shouldTransformInObject() throws TransformationException {
        JsonTransformer<ObjectToDeserialize> transformation = new JsonTransformer<>();

        ObjectToDeserialize objectToDeserialize = transformation.applyTransformation("{\"foo\":\"Foo\", \"bar\":42, \"baz\":123456789}", ObjectToDeserialize.class);
        assertEquals("Foo", objectToDeserialize.getFoo());
        assertEquals(42, objectToDeserialize.getBar());
        assertEquals(123456789, objectToDeserialize.getBaz());
    }

    @Test
    public void transform_badJson_shouldThrowException() {
        JsonTransformer<ObjectToDeserialize> transformation = new JsonTransformer<>();

        assertThrows(TransformationException.class, () -> transformation.applyTransformation("{\"fo\":\"Foo\", \"bat\":42, \"bau\":123456789}", ObjectToDeserialize.class));
    }
}
