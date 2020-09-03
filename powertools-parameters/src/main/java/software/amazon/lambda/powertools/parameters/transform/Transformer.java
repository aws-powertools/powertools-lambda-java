package software.amazon.lambda.powertools.parameters.transform;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;

public interface Transformer<T> {

    Class<JsonTransformer> json = JsonTransformer.class;
    Class<Base64Transformer> base64 = Base64Transformer.class;

    T applyTransformation(String value, Class<T> targetClass) throws TransformationException;
}
