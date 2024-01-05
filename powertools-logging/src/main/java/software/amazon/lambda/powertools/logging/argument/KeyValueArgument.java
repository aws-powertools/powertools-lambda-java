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

package software.amazon.lambda.powertools.logging.argument;

import java.util.Objects;
import software.amazon.lambda.powertools.logging.internal.JsonSerializer;

/**
 * See {@link StructuredArguments#entry(String, Object)}
 */
public class KeyValueArgument implements StructuredArgument {
    private final String key;
    private final Object value;

    public KeyValueArgument(String key, Object value) {
        this.key = Objects.requireNonNull(key, "Key must not be null");
        this.value = value;
    }

    @Override
    public void writeTo(JsonSerializer serializer) {
        serializer.writeObjectField(key, value);
    }

    @Override
    public String toString() {
        return key + "=" + StructuredArguments.toString(value);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
