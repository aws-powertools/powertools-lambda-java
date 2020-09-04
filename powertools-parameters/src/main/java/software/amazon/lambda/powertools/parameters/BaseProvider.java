/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package software.amazon.lambda.powertools.parameters;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;
import software.amazon.lambda.powertools.parameters.internal.DataStore;
import software.amazon.lambda.powertools.parameters.transform.BasicTransformer;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Base class for all parameter providers.
 */
@NotThreadSafe
public abstract class BaseProvider {

    static final int DEFAULT_MAX_AGE_SECS = 5;

    private final DataStore store = new DataStore();
    private int defaultMaxAge = DEFAULT_MAX_AGE_SECS;
    private int maxAge = defaultMaxAge;
    private Class<? extends Transformer> transformerClass;

    /**
     * Retrieve the parameter value from the underlying parameter store.<br />
     * Abstract: Implement this method in a child class of {@link BaseProvider}
     *
     * @param key key of the parameter
     * @return the value of the parameter identified by the key
     */
    abstract String getValue(String key);

    /**
     * (Optional) Set the default max age (in seconds) for the cache of all parameters. Override the default 5 seconds.<br/>
     * If for some parameters, you need to set a different maxAge, use {@link #withMaxAge(int)}.
     *
     * @param maxAgeinSec Maximum time (in seconds) to cache the parameter, before calling the underlying parameter store.
     * @return the provider itself in order to chain calls (eg. <code>provider.defaultMaxAge(10).get("key")</code>).
     */
    public <T extends BaseProvider> BaseProvider defaultMaxAge(int maxAgeinSec) {
        this.defaultMaxAge = maxAgeinSec;
        this.maxAge = maxAgeinSec;
        return this;
    }

    /**
     * (Optional) Builder method to call before {@link #get(String)} or {@link #get(String, Class)}
     * to set cache max age (in seconds) for the parameter to get.<br/><br/>
     * The max age is reset to default (either 5 or a custom value set with {@link #defaultMaxAge}) after each get,
     * so you need to use this method for each parameter to cache with non-default max age.<br/><br/>
     *
     * <b>Not Thread Safe</b>: calling this method simultaneously by several threads
     * can lead to unwanted cache time for some parameters.<br/>
     *
     * @param maxAgeinSec Maximum time (in seconds) to cache the parameter, before calling the underlying parameter store.
     * @return the provider itself in order to chain calls (eg. <code>provider.withMaxAge(10).get("key")</code>).
     */
    public <T extends BaseProvider> BaseProvider withMaxAge(int maxAgeinSec) {
        this.maxAge = maxAgeinSec;
        return this;
    }

    /**
     * Builder method to call before {@link #get(String)} (Optional) or {@link #get(String, Class)} (Mandatory).
     * to provide a {@link Transformer} that will transform the String parameter into something else (String, Object, ...)<br/><br/>
     *
     * {@link software.amazon.lambda.powertools.parameters.transform.Base64Transformer} and {@link software.amazon.lambda.powertools.parameters.transform.JsonTransformer}
     * are provided for respectively base64 and json content. You can also write your own (see {@link Transformer}).
     *
     * <b>Not Thread Safe</b>: calling this method simultaneously by several threads
     * can lead to errors (one Transformer for the wrong target type)<br/>
     *
     * @param transformerClass Class of the transformer to apply. For convenience, you can use {@link Transformer#json} or {@link Transformer#base64} shortcuts.
     * @return the provider itself in order to chain calls (eg. <code>provider.withTransformation(json).get("key", MyObject.class)</code>).
     */
    public <T extends BaseProvider> BaseProvider withTransformation(Class<? extends Transformer> transformerClass) {
        this.transformerClass = transformerClass;
        return this;
    }

    /**
     * Get the value of a parameter, either from the underlying store or a cached value (if not expired).<br/>
     * Using this method, you can apply a basic transformation (to String). <br/>
     * Set a {@link BasicTransformer} with {@link #withTransformation(Class)}.<br/><br/>
     * If you need a more complex transformation (to Object), use {@link #get(String, Class)} method instead of this one. <br/>
     *
     * @param key key of the parameter
     * @return the String value of the parameter
     */
    public String get(String key) {
        if (hasNotExpired(key)) {
            resetToDefaults();
            return (String) store.get(key);
        }

        String value = getValue(key);

        String transformedValue = value;
        if (transformerClass != null) {
            if (!BasicTransformer.class.isAssignableFrom(transformerClass)) {
                resetToDefaults();
                throw new IllegalArgumentException("Wrong Transformer for a String, choose a BasicTransformer");
            }
            try {
                BasicTransformer transformer = (BasicTransformer) transformerClass.newInstance();
                transformedValue = transformer.applyTransformation(value);
            } catch (InstantiationException | IllegalAccessException e) {
                resetToDefaults();
                throw new TransformationException(e);
            }
        }

        store.put(key, transformedValue, Instant.now().plus(maxAge, ChronoUnit.SECONDS).toEpochMilli());

        resetToDefaults();
        return transformedValue;
    }

    /**
     * Get the value of a parameter, either from the underlying store or a cached value (if not expired).<br/>
     * Using this method, you must apply a transformation (eg. json/xml to Object). <br/>
     * Set a {@link Transformer} with {@link #withTransformation(Class)}.<br/><br/>
     * If you need a simpler transformation (to String), use {@link #get(String)} method instead of this one.
     *
     * @param key         key of the parameter
     * @param targetClass class of the target Object (after transformation)
     * @return the Object (T) value of the parameter
     */
    public <T> T get(String key, Class<T> targetClass) {
        if (hasNotExpired(key)) {
            resetToDefaults();
            return (T) store.get(key);
        }

        String value = getValue(key);

        if (transformerClass == null) {
            resetToDefaults();
            throw new IllegalArgumentException("transformer is null, use withTransformation to specify a transformer");
        }
        Transformer<T> transformer;
        try {
            transformer = transformerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            resetToDefaults();
            throw new TransformationException(e);
        }
        T transformedValue = transformer.applyTransformation(value, targetClass);

        store.put(key, transformedValue, Instant.now().plus(maxAge, ChronoUnit.SECONDS).toEpochMilli());

        resetToDefaults();
        return transformedValue;
    }

    boolean hasNotExpired(String key) {
        return store.hasNotExpired(key);
    }

    private void resetToDefaults() {
        transformerClass = null;
        maxAge = defaultMaxAge;
    }
}
