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

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.Objects;

public class JsonArgument implements StructuredArgument {
    private final String key;
    private final String rawJson;

    public JsonArgument(String key, String rawJson) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.rawJson = Objects.requireNonNull(rawJson, "rawJson must not be null");;
    }

    @Override
    public void writeTo(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(key);
        jsonGenerator.writeRawValue(rawJson);
    }

    @Override
    public String toString() {
        return key + "=" + rawJson;
    }
}
