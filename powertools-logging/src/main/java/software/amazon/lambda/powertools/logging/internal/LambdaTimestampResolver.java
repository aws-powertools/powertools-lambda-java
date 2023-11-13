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

import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolver;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolverConfig;
import org.apache.logging.log4j.layout.template.json.util.InstantFormatter;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Default timestamp used by log4j is not RFC3339, which is used by Lambda internally to filter logs.
 * When `AWS_LAMBDA_LOG_FORMAT` is set to JSON (i.e. using Lambda logging configuration), we should use the appropriate pattern,
 * otherwise logs with invalid date format are considered as INFO.
 * Inspired from org.apache.logging.log4j.layout.template.json.resolver.TimestampResolver
 *
 * TODO: remove in v2 an replace with the good pattern in LambdaJsonLayout.json
 */
public class LambdaTimestampResolver implements EventResolver {

    private static final String DATE_RFC3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private final EventResolver internalResolver;

    public LambdaTimestampResolver(final TemplateResolverConfig config) {
        final PatternResolverContext patternResolverContext =
                PatternResolverContext.fromConfig(config);
        internalResolver = new PatternResolver(patternResolverContext);
    }

    @Override
    public void resolve(LogEvent value, JsonWriter jsonWriter) {
        internalResolver.resolve(value, jsonWriter);
    }

    static String getName() {
        return "lambda-timestamp";
    }

    private static final class PatternResolverContext {

        public static final String PATTERN = "pattern";
        private final InstantFormatter formatter;

        private final StringBuilder lastFormattedInstantBuffer = new StringBuilder();

        private final MutableInstant lastFormattedInstant = new MutableInstant();

        private PatternResolverContext(
                final String pattern,
                final TimeZone timeZone,
                final Locale locale) {
            this.formatter = InstantFormatter
                    .newBuilder()
                    .setPattern(pattern)
                    .setTimeZone(timeZone)
                    .setLocale(locale)
                    .build();
            lastFormattedInstant.initFromEpochSecond(-1, 0);
        }

        private static PatternResolverContext fromConfig(
                final TemplateResolverConfig config) {
            final String pattern = readPattern(config);
            final TimeZone timeZone = readTimeZone(config);
            final Locale locale = config.getLocale(new String[]{PATTERN, "locale"});
            return new PatternResolverContext(pattern, timeZone, locale);
        }

        private static String readPattern(final TemplateResolverConfig config) {
            final String format = config.getString(new String[]{PATTERN, "format"});
            return format != null
                    ? format
                    : getLambdaTimestampFormatOrDefault();
        }

        private static String getLambdaTimestampFormatOrDefault() {
            String logFormat = System.getenv("AWS_LAMBDA_LOG_FORMAT");
            return "JSON".equals(logFormat) ? DATE_RFC3339_FORMAT :
                    JsonTemplateLayoutDefaults.getTimestampFormatPattern();
        }

        private static TimeZone readTimeZone(final TemplateResolverConfig config) {
            final String timeZoneId = config.getString(new String[]{PATTERN, "timeZone"});
            if (timeZoneId == null) {
                return JsonTemplateLayoutDefaults.getTimeZone();
            }
            boolean found = false;
            for (final String availableTimeZone : TimeZone.getAvailableIDs()) {
                if (availableTimeZone.equalsIgnoreCase(timeZoneId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(
                        "invalid timestamp time zone: " + config);
            }
            return TimeZone.getTimeZone(timeZoneId);
        }

    }

    private static final class PatternResolver implements EventResolver {

        private final PatternResolverContext patternResolverContext;

        private PatternResolver(final PatternResolverContext patternResolverContext) {
            this.patternResolverContext = patternResolverContext;
        }

        @Override
        public synchronized void resolve(
                final LogEvent logEvent,
                final JsonWriter jsonWriter) {

            // Format timestamp if it doesn't match the last cached one.
            final boolean instantMatching = patternResolverContext.formatter.isInstantMatching(
                    patternResolverContext.lastFormattedInstant,
                    logEvent.getInstant());
            if (!instantMatching) {

                // Format the timestamp.
                patternResolverContext.lastFormattedInstantBuffer.setLength(0);
                patternResolverContext.lastFormattedInstant.initFrom(logEvent.getInstant());
                patternResolverContext.formatter.format(
                        patternResolverContext.lastFormattedInstant,
                        patternResolverContext.lastFormattedInstantBuffer);

                // Write the formatted timestamp.
                final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
                final int startIndex = jsonWriterStringBuilder.length();
                jsonWriter.writeString(patternResolverContext.lastFormattedInstantBuffer);

                // Cache the written value.
                patternResolverContext.lastFormattedInstantBuffer.setLength(0);
                patternResolverContext.lastFormattedInstantBuffer.append(
                        jsonWriterStringBuilder,
                        startIndex,
                        jsonWriterStringBuilder.length());

            }

            // Write the cached formatted timestamp.
            else {
                jsonWriter.writeRawString(
                        patternResolverContext.lastFormattedInstantBuffer);
            }

        }

    }
}
