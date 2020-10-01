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
package software.amazon.lambda.powertools.parameters.transform;

import software.amazon.lambda.powertools.parameters.exception.TransformationException;

/**
 * Interface for parameter transformers. Implement it to create a new Transformer.
 *
 * @param <T> type of the target object that will be created with the transformer.
 */
public interface Transformer<T> {

    /**
     * Convenient access to {@link JsonTransformer}, to use in providers (<pre>provider.withTransformation(json)</pre>)
     */
    Class<JsonTransformer> json = JsonTransformer.class;

    /**
     * Convenient access to {@link Base64Transformer}, to use in providers (<pre>provider.withTransformation(base64)</pre>)
     */
    Class<Base64Transformer> base64 = Base64Transformer.class;

    /**
     * Apply a transformation on the input value (String)
     * @param value the parameter value to transform
     * @param targetClass class of the target object
     * @return a transformed parameter
     * @throws TransformationException when a transformation error occurs
     */
    T applyTransformation(String value, Class<T> targetClass) throws TransformationException;
}
