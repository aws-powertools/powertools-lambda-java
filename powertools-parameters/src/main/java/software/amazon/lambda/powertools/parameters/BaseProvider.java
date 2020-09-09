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

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Base class for all parameter providers.
 */
@NotThreadSafe
public abstract class BaseProvider {

    static final Duration DEFAULT_MAX_AGE_SECS = Duration.of(5, SECONDS);

    private final DataStore store = new DataStore();
    private Duration defaultMaxAge = DEFAULT_MAX_AGE_SECS;
    private Duration maxAge = defaultMaxAge;
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
     * (Optional) Set the default max age for the cache of all parameters. Override the default 5 seconds.<br/>
     * If for some parameters, you need to set a different maxAge, use {@link #withMaxAge(int, ChronoUnit)}.
     *
     * @param maxAge Maximum time to cache the parameter, before calling the underlying parameter store.
     * @param unit Unit of time
     * @return the provider itself in order to chain calls (eg. <code>provider.defaultMaxAge(10, SECONDS).get("key")</code>).
     */
    public BaseProvider defaultMaxAge(int maxAge, ChronoUnit unit) {
        Duration duration = Duration.of(maxAge, unit);
        this.defaultMaxAge = duration;
        this.maxAge = duration;
        return this;
    }

    /**
     * (Optional) Builder method to call before {@link #get(String)} or {@link #get(String, Class)}
     * to set cache max age for the parameter to get.<br/><br/>
     * The max age is reset to default (either 5 or a custom value set with {@link #defaultMaxAge}) after each get,
     * so you need to use this method for each parameter to cache with non-default max age.<br/><br/>
     *
     * <b>Not Thread Safe</b>: calling this method simultaneously by several threads
     * can lead to unwanted cache time for some parameters.<br/>
     *
     * @param maxAge Maximum time to cache the parameter, before calling the underlying parameter store.
     * @param unit Unit of time
     * @return the provider itself in order to chain calls (eg. <code>provider.withMaxAge(10, SECONDS).get("key")</code>).
     */
    public BaseProvider withMaxAge(int maxAge, ChronoUnit unit) {
        this.maxAge = Duration.of(maxAge, unit);
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
    public BaseProvider withTransformation(Class<? extends Transformer> transformerClass) {
        this.transformerClass = transformerClass;
        return this;
    }

    /**
     * Abstract method to tell to retrieve parameters recursively. Only available for {@link SSMProvider}.
     * @return the provider itself in order to chain calls
     */
    public BaseProvider recursive() {
        return this;
    }

    /**
     * Abstract method to tell to decrypt parameters. Only available for {@link SSMProvider}.
     * @return the provider itself in order to chain calls
     */
    public BaseProvider withDecryption() {
        return this;
    }

    /**
     * Get the value of a parameter, either from the underlying store or a cached value (if not expired).<br/>
     * Using this method, you can apply a basic transformation (to String). <br/>
     * Set a {@link BasicTransformer} with {@link #withTransformation(Class)}.<br/><br/>
     * If you need a more complex transformation (to Object), use {@link #get(String, Class)} method instead of this one. <br/>
     *
     * @param key key of the parameter
     * @throws IllegalArgumentException if a wrong transformer class is provided through {@link #withTransformation(Class)}. Needs to be a {@link BasicTransformer}.
     * @throws TransformationException if the transformation could not be done, because of a wrong format or an error during transformation.
     * @return the String value of the parameter
     */
    public String get(String key) {
        if (!hasExpired(key)) {
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
                BasicTransformer transformer = (BasicTransformer) transformerClass.getDeclaredConstructor().newInstance(null);
                transformedValue = transformer.applyTransformation(value);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                resetToDefaults();
                throw new TransformationException(e);
            }
        }

        store.put(key, transformedValue, Instant.now().plus(maxAge));

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
     * @throws IllegalArgumentException if no transformation class was provided through {@link #withTransformation(Class)}
     * @throws TransformationException if the transformation could not be done, because of a wrong format or an error during transformation.
     * @return the Object (T) value of the parameter
     */
    public <T> T get(String key, Class<T> targetClass) {
        if (! hasExpired(key)) {
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
            transformer = transformerClass.getDeclaredConstructor().newInstance(null);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            resetToDefaults();
            throw new TransformationException(e);
        }
        T transformedValue = transformer.applyTransformation(value, targetClass);

        store.put(key, transformedValue, Instant.now().plus(maxAge));

        resetToDefaults();
        return transformedValue;
    }

    boolean hasExpired(String key) {
        return store.hasExpired(key);
    }

    protected void resetToDefaults() {
        transformerClass = null;
        maxAge = defaultMaxAge;
    }
}
