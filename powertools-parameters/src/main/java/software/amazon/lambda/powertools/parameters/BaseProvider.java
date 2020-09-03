package software.amazon.lambda.powertools.parameters;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.transform.BasicTransformer;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseProvider {

    static final int DEFAULT_MAX_AGE_SECS = 5;

    private final Map<String, Long> timeMap = new ConcurrentHashMap<>();
    private final Map<String, Object> store = new ConcurrentHashMap<>();

    private int maxAge = DEFAULT_MAX_AGE_SECS;
    private Class<? extends Transformer> transformerClass;

    abstract String getValue(String key);

    public <T extends BaseProvider> BaseProvider withMaxAge(int maxAgeinSec) {
        this.maxAge = maxAgeinSec;
        return this;
    }

    public <T extends BaseProvider> BaseProvider withTransformation(Class<? extends Transformer> transformerClass) {
        this.transformerClass = transformerClass;
        return this;
    }

    public String get(String key) {
        if (hasNotExpired(key)) {
            return (String) store.get(key);
        }

        String value = getValue(key);

        String transformedValue = value;
        if (transformerClass != null) {
            if (!BasicTransformer.class.isAssignableFrom(transformerClass)) {
                throw new IllegalArgumentException("Wrong Transformer for a String, choose a BasicTransformer");
            }
            try {
                BasicTransformer transformer = (BasicTransformer) transformerClass.newInstance();
                transformedValue = transformer.applyTransformation(value);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new TransformationException(e);
            }
        }

        store.put(key, transformedValue);
        timeMap.put(key, Instant.now().plus(maxAge, ChronoUnit.SECONDS).toEpochMilli());

        return transformedValue;
    }

    public <T> T get(String key, Class<T> targetClass) {
        if (hasNotExpired(key)) {
            return (T) store.get(key);
        }

        String value = getValue(key);

        if (transformerClass == null) {
            throw new IllegalArgumentException("transformer is null, use withTransformation to specify a transformer");
        }
        Transformer transformer;
        try {
            transformer = transformerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TransformationException(e);
        }
        Object transformedValue = transformer.applyTransformation(value, targetClass);

        store.put(key, transformedValue);
        timeMap.put(key, Instant.now().plus(maxAge, ChronoUnit.SECONDS).toEpochMilli());

        return (T) transformedValue;
    }

    boolean hasNotExpired(String key) {
        return store.containsKey(key) && timeMap.containsKey(key) && timeMap.get(key) >= Instant.now().toEpochMilli();
    }


//    public abstract Map<String, String> getMultiple(String path);
}
