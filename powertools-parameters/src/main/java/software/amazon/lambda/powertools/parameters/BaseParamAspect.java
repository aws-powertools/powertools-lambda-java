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

public class BaseParamAspect {

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
