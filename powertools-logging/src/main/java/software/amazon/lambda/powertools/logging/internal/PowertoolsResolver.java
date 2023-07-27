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

package software.amazon.lambda.powertools.logging.internal;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolver;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

final class PowertoolsResolver implements EventResolver {

    private final EventResolver internalResolver;

    PowertoolsResolver() {
        internalResolver = new EventResolver() {
            @Override
            public boolean isResolvable(LogEvent value) {
                ReadOnlyStringMap contextData = value.getContextData();
                return null != contextData && !contextData.isEmpty();
            }

            @Override
            public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
                StringBuilder stringBuilder = jsonWriter.getStringBuilder();
                // remove dummy field to kick inn powertools resolver
                stringBuilder.setLength(stringBuilder.length() - 4);

                // Inject all the context information.
                ReadOnlyStringMap contextData = logEvent.getContextData();
                contextData.forEach((key, value) ->
                    {
                        jsonWriter.writeSeparator();
                        jsonWriter.writeString(key);
                        stringBuilder.append(':');
                        jsonWriter.writeValue(value);
                    });
            }
        };
    }

    static String getName() {
        return "powertools";
    }

    @Override
    public void resolve(LogEvent value, JsonWriter jsonWriter) {
        internalResolver.resolve(value, jsonWriter);
    }

    @Override
    public boolean isResolvable(LogEvent value) {
        return internalResolver.isResolvable(value);
    }
}
