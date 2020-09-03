package software.amazon.lambda.powertools.parameters.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;

public class JsonTransformer<T> implements Transformer<T> {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public T applyTransformation(String value, Class<T> targetClass) throws TransformationException {
        try {
            return mapper.readValue(value, targetClass);
        } catch (JsonProcessingException e) {
            throw new TransformationException(e);
        }
    }
}
