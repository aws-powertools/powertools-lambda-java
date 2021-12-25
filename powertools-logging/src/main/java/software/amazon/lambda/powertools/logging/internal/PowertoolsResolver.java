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
                contextData.forEach((key, value) -> {
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
