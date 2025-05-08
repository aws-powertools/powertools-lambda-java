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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import software.amazon.lambda.powertools.logging.internal.JsonSerializer;

/**
 * See {@link StructuredArguments#entries(Map)}
 */
class MapArgument implements StructuredArgument {
    private final Map<?, ?> map;

    public MapArgument(Map<?, ?> map) {
        if (map != null) {
            this.map = new HashMap<>(map);
        } else {
            this.map = null;
        }
    }

    @Override
    public void writeTo(JsonSerializer serializer) {
        if (map != null) {
            for (Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator(); entries.hasNext();) {
                Map.Entry<?, ?> entry = entries.next();
                serializer.writeObjectField(String.valueOf(entry.getKey()), entry.getValue());
                // If the map has more than one entry, we need to print a (comma) separator to avoid breaking the JSON
                if (entries.hasNext()) {
                    serializer.writeSeparator();
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.valueOf(map);
    }

}
