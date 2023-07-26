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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.parameters.exception.TransformationException;

/**
 * Transformer that transform a json string into an Object. Based on Jackson.
 *
 * @param <T> type of the Object to create during transformation.
 */
public class JsonTransformer<T> implements Transformer<T> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public T applyTransformation(String value, Class<T> targetClass) throws TransformationException {
        try {
            return mapper.readValue(value, targetClass);
        } catch (JsonProcessingException e) {
            throw new TransformationException(e);
        }
    }
}
