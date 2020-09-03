package software.amazon.lambda.powertools.parameters.transform;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;

public abstract class BasicTransformer implements Transformer<String> {

    @Override
    public String applyTransformation(String value, Class<String> targetClass) throws TransformationException {
        return applyTransformation(value);
    }

    public abstract String applyTransformation(String value);
}
