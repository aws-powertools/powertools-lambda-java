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
package software.amazon.lambda.powertools.logging.internal;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

/***
 *  Note: The LambdaJsonLayout should be considered to be deprecated. Please use JsonTemplateLayout instead.
 */
@Deprecated
@Plugin(name = "LambdaJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class LambdaJsonLayout extends AbstractJacksonLayoutCopy {
    private static final String DEFAULT_FOOTER = "]";

    private static final String DEFAULT_HEADER = "[";

    static final String CONTENT_TYPE = "application/json";

    public static class Builder<B extends Builder<B>> extends AbstractJacksonLayoutCopy.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<LambdaJsonLayout> {

        @PluginBuilderAttribute
        private boolean propertiesAsList;

        @PluginBuilderAttribute
        private boolean objectMessageAsJsonObject;

        public Builder() {
            super();
            setCharset(StandardCharsets.UTF_8);
        }

        @Override
        public LambdaJsonLayout build() {
            final boolean encodeThreadContextAsList = isProperties() && propertiesAsList;
            final String headerPattern = toStringOrNull(getHeader());
            final String footerPattern = toStringOrNull(getFooter());
            return new LambdaJsonLayout(getConfiguration(), isLocationInfo(), isProperties(), encodeThreadContextAsList,
                    isComplete(), isCompact(), getEventEol(), headerPattern, footerPattern, getCharset(),
                    isIncludeStacktrace(), isStacktraceAsString(), isIncludeNullDelimiter(),
                    getAdditionalFields(), getObjectMessageAsJsonObject());
        }

        public boolean isPropertiesAsList() {
            return propertiesAsList;
        }

        public B setPropertiesAsList(final boolean propertiesAsList) {
            this.propertiesAsList = propertiesAsList;
            return asBuilder();
        }

        public boolean getObjectMessageAsJsonObject() {
            return objectMessageAsJsonObject;
        }

        public B setObjectMessageAsJsonObject(final boolean objectMessageAsJsonObject) {
            this.objectMessageAsJsonObject = objectMessageAsJsonObject;
            return asBuilder();
        }
    }

    private LambdaJsonLayout(final Configuration config, final boolean locationInfo, final boolean properties,
                             final boolean encodeThreadContextAsList,
                             final boolean complete, final boolean compact, final boolean eventEol,
                             final String headerPattern, final String footerPattern, final Charset charset,
                             final boolean includeStacktrace, final boolean stacktraceAsString,
                             final boolean includeNullDelimiter,
                             final KeyValuePair[] additionalFields, final boolean objectMessageAsJsonObject) {
        super(config, new JacksonFactoryCopy.JSON(encodeThreadContextAsList, includeStacktrace, stacktraceAsString, objectMessageAsJsonObject).newWriter(
                locationInfo, properties, compact),
                charset, compact, complete, eventEol,
                null,
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build(),
                includeNullDelimiter,
                additionalFields);
    }

    /**
     * Returns appropriate JSON header.
     *
     * @return a byte array containing the header, opening the JSON array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final String str = serializeToString(getHeaderSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    /**
     * Returns appropriate JSON footer.
     *
     * @return a byte array containing the footer, closing the JSON array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(this.eol);
        final String str = serializeToString(getFooterSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("version", "2.0");
        return result;
    }

    /**
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Creates a JSON Layout using the default settings. Useful for testing.
     *
     * @return A JSON Layout.
     */
    public static LambdaJsonLayout createDefaultLayout() {
        return new LambdaJsonLayout(new DefaultConfiguration(), false, false, false, false, false, false,
                DEFAULT_HEADER, DEFAULT_FOOTER, StandardCharsets.UTF_8, true, false, false, null, false);
    }

    @Override
    public Object wrapLogEvent(final LogEvent event) {
        Map<String, Object> additionalFieldsMap = getAdditionalFields(event);
        // This class combines LogEvent with AdditionalFields during serialization
        return new LogEventWithAdditionalFields(event, additionalFieldsMap);
    }

    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }

    private Map<String, Object> getAdditionalFields(LogEvent logEvent) {
        // Note: LinkedHashMap retains order
        final Map<String, Object> additionalFieldsMap = new LinkedHashMap<>(additionalFields.length);

        // Go over MDC
        logEvent.getContextData().forEach((key, value) -> {
            if (Strings.isNotBlank(key) && value != null) {
                additionalFieldsMap.put(key, value);
            }
        });

        return additionalFieldsMap;
    }

    @JsonRootName(XmlConstants.ELT_EVENT)
    public static class LogEventWithAdditionalFields {

        private final LogEvent logEvent;
        private final Map<String, Object> additionalFields;

        public LogEventWithAdditionalFields(LogEvent logEvent, Map<String, Object> additionalFields) {
            this.logEvent = logEvent;
            this.additionalFields = additionalFields;
        }

        @JsonUnwrapped
        public Object getLogEvent() {
            return logEvent;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalFields() {
            return additionalFields;
        }

        @JsonGetter("timestamp")
        public String getTimestamp() {
            return ISO_ZONED_DATE_TIME.format(ZonedDateTime.from(ofEpochMilli(logEvent.getTimeMillis()).atZone(ZoneId.systemDefault())));
        }
    }
}
