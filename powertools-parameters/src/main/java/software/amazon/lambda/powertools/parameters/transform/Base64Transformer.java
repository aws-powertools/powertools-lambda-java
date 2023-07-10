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

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Transformer that take a base64 encoded string and return a decoded string.
 */
public class Base64Transformer extends BasicTransformer {

    @Override
    public String applyTransformation(String value) throws TransformationException {
        try {
            return new String(Base64.getDecoder().decode(value), UTF_8);
        } catch (Exception e) {
            throw new TransformationException(e);
        }
    }
}
