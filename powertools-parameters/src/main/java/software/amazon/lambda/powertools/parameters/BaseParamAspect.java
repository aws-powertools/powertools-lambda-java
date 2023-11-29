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

package software.amazon.lambda.powertools.parameters;

import org.aspectj.lang.reflect.FieldSignature;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

/**
 * Provides a common base for all parameter aspects. This lets us group functionality that
 * we need to reimplement in each aspect. This class should be extended for each
 * additional parameter aspect.
 */
public abstract class BaseParamAspect {

    /**
     * Gets the parameter value from the provider and transforms it if necessary. This transformation
     * is generic across all parameter providers.
     *
     * @param key The key of the parameter to get
     * @param transformer The transformer to use to transform the parameter value
     * @param provider A concrete instance of the parameter provider to retrieve the value from
     * @param fieldSignature The signature of the field that the parameter is being injected into
     *
     * @return The value of the parameter, transformed if necessary
     */
    protected Object getAndTransform(String key, Class<? extends Transformer> transformer, BaseProvider provider,
                                     FieldSignature fieldSignature) {
        if (transformer.isInterface()) {
            // No transformation
            return provider.get(key);
        } else {
            Class fieldType = fieldSignature.getFieldType();
            if (String.class.isAssignableFrom(fieldType)) {
                // Basic transformation
                return provider
                        .withTransformation(transformer)
                        .get(key);
            } else {
                // Complex transformation
                return provider
                        .withTransformation(transformer)
                        .get(key, fieldType);
            }
        }
    }
}
