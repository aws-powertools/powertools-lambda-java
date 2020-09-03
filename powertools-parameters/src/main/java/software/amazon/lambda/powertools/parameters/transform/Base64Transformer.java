package software.amazon.lambda.powertools.parameters.transform;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;

import java.util.Base64;

public class Base64Transformer extends BasicTransformer {

    @Override
    public String applyTransformation(String value) throws TransformationException {
        try {
            return new String(Base64.getDecoder().decode(value));
        } catch (Exception e) {
            throw new TransformationException(e);
        }
    }
}
