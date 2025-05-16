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
 */
package software.amazon.lambda.powertools.kafka.serializers;

import java.io.InputStream;
import java.lang.reflect.Type;

import com.amazonaws.services.lambda.runtime.serialization.factories.JacksonFactory;

public class LambdaDefaultDeserializer implements PowertoolsDeserializer {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(InputStream input, Type type) {
        return JacksonFactory.getInstance().getSerializer((Class<T>) type).fromJson(input);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fromJson(String input, Type type) {
        return JacksonFactory.getInstance().getSerializer((Class<T>) type).fromJson(input);
    }
}
