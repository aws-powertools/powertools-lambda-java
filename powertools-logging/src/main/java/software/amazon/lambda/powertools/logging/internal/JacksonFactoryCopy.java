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

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.JsonConstants;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;

@Deprecated
abstract class JacksonFactoryCopy {

    abstract protected String getPropertyNameForTimeMillis();

    abstract protected String getPropertyNameForInstant();

    abstract protected String getPropertNameForContextMap();

    abstract protected String getPropertNameForSource();

    abstract protected String getPropertNameForNanoTime();

    abstract protected PrettyPrinter newCompactPrinter();

    abstract protected ObjectMapper newObjectMapper();

    abstract protected PrettyPrinter newPrettyPrinter();

    ObjectWriter newWriter(final boolean locationInfo, final boolean properties, final boolean compact) {
        return newWriter(locationInfo, properties, compact, false);
    }

    ObjectWriter newWriter(final boolean locationInfo, final boolean properties, final boolean compact,
                           final boolean includeMillis) {
        final SimpleFilterProvider filters = new SimpleFilterProvider();
        final Set<String> except = new HashSet<>(3);
        if (!locationInfo) {
            except.add(this.getPropertNameForSource());
        }
        if (!properties) {
            except.add(this.getPropertNameForContextMap());
        }
        if (includeMillis) {
            except.add(getPropertyNameForInstant());
        } else {
            except.add(getPropertyNameForTimeMillis());
        }
        except.add(this.getPropertNameForNanoTime());
        filters.addFilter(Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
        final ObjectWriter writer =
                this.newObjectMapper().writer(compact ? this.newCompactPrinter() : this.newPrettyPrinter());
        return writer.with(filters);
    }

    static class JSON extends JacksonFactoryCopy {

        private final boolean encodeThreadContextAsList;
        private final boolean includeStacktrace;
        private final boolean stacktraceAsString;
        private final boolean objectMessageAsJsonObject;

        public JSON(final boolean encodeThreadContextAsList, final boolean includeStacktrace,
                    final boolean stacktraceAsString, final boolean objectMessageAsJsonObject) {
            this.encodeThreadContextAsList = encodeThreadContextAsList;
            this.includeStacktrace = includeStacktrace;
            this.stacktraceAsString = stacktraceAsString;
            this.objectMessageAsJsonObject = objectMessageAsJsonObject;
        }

        @Override
        protected String getPropertNameForContextMap() {
            return JsonConstants.ELT_CONTEXT_MAP;
        }

        @Override
        protected String getPropertyNameForTimeMillis() {
            return JsonConstants.ELT_TIME_MILLIS;
        }

        @Override
        protected String getPropertyNameForInstant() {
            return JsonConstants.ELT_INSTANT;
        }

        @Override
        protected String getPropertNameForSource() {
            return JsonConstants.ELT_SOURCE;
        }

        @Override
        protected String getPropertNameForNanoTime() {
            return JsonConstants.ELT_NANO_TIME;
        }

        @Override
        protected PrettyPrinter newCompactPrinter() {
            return new MinimalPrettyPrinter();
        }

        @Override
        protected ObjectMapper newObjectMapper() {
            return new Log4jJsonObjectMapper(encodeThreadContextAsList, includeStacktrace, stacktraceAsString,
                    objectMessageAsJsonObject);
        }

        @Override
        protected PrettyPrinter newPrettyPrinter() {
            return new DefaultPrettyPrinter();
        }

    }

}