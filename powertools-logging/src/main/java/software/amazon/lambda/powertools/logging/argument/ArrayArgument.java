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
import java.util.Set;

import software.amazon.lambda.powertools.logging.internal.JsonSerializer;

/**
 * See {@link StructuredArguments#array(String, Object...)}
 */
class ArrayArgument implements StructuredArgument {
    private final String key;
    private final Object[] values;

    public ArrayArgument(String key, Object[] values) {
        this.key = Objects.requireNonNull(key, "Key must not be null");
        this.values = new Object[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    @Override
    public void writeTo(JsonSerializer serializer) {
        serializer.writeFieldName(key);
        serializer.writeObject(values);
    }

    @Override
    public String toString() {
        return key + "=" + StructuredArguments.toString(values);
    }

    @Override
    public Iterable<String> keys() {
        return Set.of(key);
    }
}
