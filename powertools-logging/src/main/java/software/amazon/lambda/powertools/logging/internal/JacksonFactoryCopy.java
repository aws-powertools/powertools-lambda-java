package software.amazon.lambda.powertools.logging.internal;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.JsonConstants;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;

import java.util.HashSet;
import java.util.Set;

abstract class JacksonFactoryCopy {

    static class JSON extends JacksonFactoryCopy {

        private final boolean encodeThreadContextAsList;
        private final boolean includeStacktrace;
        private final boolean stacktraceAsString;
        private final boolean objectMessageAsJsonObject;

        public JSON(final boolean encodeThreadContextAsList, final boolean includeStacktrace, final boolean stacktraceAsString, final boolean objectMessageAsJsonObject) {
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
            return new Log4jJsonObjectMapper(encodeThreadContextAsList, includeStacktrace, stacktraceAsString, objectMessageAsJsonObject);
        }

        @Override
        protected PrettyPrinter newPrettyPrinter() {
            return new DefaultPrettyPrinter();
        }

    }

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
        final ObjectWriter writer = this.newObjectMapper().writer(compact ? this.newCompactPrinter() : this.newPrettyPrinter());
        return writer.with(filters);
    }

}