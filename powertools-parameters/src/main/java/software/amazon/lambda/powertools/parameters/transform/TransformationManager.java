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
 */
public class TransformationManager {

    private Class<? extends Transformer> transformer = null;

    /**
     * Set the {@link Transformer} to use for transformation. Must be called before any transformation.
     *
     * @param transformerClass class of the {@link Transformer}
     */
    public void setTransformer(Class<? extends Transformer> transformerClass) {
        this.transformer = transformerClass;
    }

    /**
     * @return true if a {@link Transformer} has been passed to the Manager
     */
    public boolean shouldTransform() {
        return transformer != null;
    }

    /**
     * Transform a String in another String. Must be used with a {@link BasicTransformer}.
     *
     * @param value the value to transform
     * @return the value transformed
     */
    public String performBasicTransformation(String value) {
        if (transformer == null) {
            throw new IllegalStateException(
                    "You cannot perform a transformation without Transformer, use the provider.withTransformation() method to specify it.");
        }
        if (!BasicTransformer.class.isAssignableFrom(transformer)) {
            throw new IllegalStateException("Wrong Transformer for a String, choose a BasicTransformer.");
        }
        try {
            BasicTransformer basicTransformer =
                    (BasicTransformer) transformer.getDeclaredConstructor().newInstance(null);
            return basicTransformer.applyTransformation(value);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
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
    public <T> T performComplexTransformation(String value, Class<T> targetClass) {
        if (transformer == null) {
            throw new IllegalStateException(
                    "You cannot perform a transformation without Transformer, use the provider.withTransformation() method to specify it.");
        }

        try {
            Transformer<T> complexTransformer = transformer.getDeclaredConstructor().newInstance(null);
            return complexTransformer.applyTransformation(value, targetClass);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new TransformationException(e);
        }
    }
}
