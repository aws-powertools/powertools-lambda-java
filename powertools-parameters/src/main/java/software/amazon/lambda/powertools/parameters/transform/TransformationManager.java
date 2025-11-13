/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
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

package software.amazon.lambda.powertools.parameters.transform;

import java.lang.reflect.InvocationTargetException;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;

/**
 * Manager in charge of transforming parameter values in another format. <br/>
 * Leverages a {@link Transformer} in order to perform the transformation. <br/>
 * The transformer must be passed with {@link #setTransformer(Class)} before performing any transform operation.
 * <p>
 * This class is thread-safe. Transformer configuration is thread-local to support concurrent
 * requests with different transformation requirements.
 */
public class TransformationManager {

    private final ThreadLocal<Class<? extends Transformer>> transformer = ThreadLocal.withInitial(() -> null);

    /**
     * Set the {@link Transformer} to use for transformation. Must be called before any transformation.
     *
     * @param transformerClass class of the {@link Transformer}
     */
    @SuppressWarnings("rawtypes") // Transformer type parameter determined at runtime
    public void setTransformer(Class<? extends Transformer> transformerClass) {
        this.transformer.set(transformerClass);
    }

    /**
     * Unset the {@link Transformer} and clean up thread-local storage.
     * Should be called after transformation is complete to prevent memory leaks.
     */
    public void unsetTransformer() {
        this.transformer.remove();
    }

    /**
     * @return true if a {@link Transformer} has been passed to the Manager
     */
    public boolean shouldTransform() {
        return transformer.get() != null;
    }

    /**
     * Transform a String in another String. Must be used with a {@link BasicTransformer}.
     *
     * @param value the value to transform
     * @return the value transformed
     */
    @SuppressWarnings("rawtypes") // Transformer type parameter determined at runtime
    public String performBasicTransformation(String value) {
        Class<? extends Transformer> transformerClass = transformer.get();
        if (transformerClass == null) {
            throw new IllegalStateException(
                    "You cannot perform a transformation without Transformer, use the provider.withTransformation() method to specify it.");
        }
        if (!BasicTransformer.class.isAssignableFrom(transformerClass)) {
            throw new IllegalStateException("Wrong Transformer for a String, choose a BasicTransformer.");
        }
        try {
            BasicTransformer basicTransformer = (BasicTransformer) transformerClass.getDeclaredConstructor()
                    .newInstance(null);
            return basicTransformer.applyTransformation(value);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new TransformationException(e);
        }
    }

    /**
     * Transform a String in a Java Object.
     *
     * @param value       the value to transform
     * @param targetClass the type of the target object.
     * @return the value transformed in an object ot type T.
     */
    @SuppressWarnings("rawtypes") // Transformer type parameter determined at runtime
    public <T> T performComplexTransformation(String value, Class<T> targetClass) {
        Class<? extends Transformer> transformerClass = transformer.get();
        if (transformerClass == null) {
            throw new IllegalStateException(
                    "You cannot perform a transformation without Transformer, use the provider.withTransformation() method to specify it.");
        }

        try {
            Transformer<T> complexTransformer = transformerClass.getDeclaredConstructor().newInstance(null);
            return complexTransformer.applyTransformation(value, targetClass);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new TransformationException(e);
        }
    }
}
