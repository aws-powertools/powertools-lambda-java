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

package software.amazon.lambda.powertools.logging.log4j;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;

/**
 * A minimalistic Log4j2 appender that buffers log events based on trace ID
 * and flushes them when error logs are encountered or manually triggered.
 */
@Plugin(name = "BufferingAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class BufferingAppender extends AbstractAppender {

    private final AppenderRef[] appenderRefs;
    private final Configuration configuration;
    private final Level bufferAtVerbosity;
    private final int maxBytes;
    private final boolean flushOnErrorLog;
    private final Map<String, Deque<LogEvent>> bufferCache = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> bufferOverflowWarned = new ThreadLocal<>();

    protected BufferingAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            AppenderRef[] appenderRefs, Configuration configuration, Level bufferAtVerbosity, int maxBytes,
            boolean flushOnErrorLog) {
        super(name, filter, layout, false, null);
        this.appenderRefs = appenderRefs;
        this.configuration = configuration;
        this.bufferAtVerbosity = bufferAtVerbosity;
        this.maxBytes = maxBytes;
        this.flushOnErrorLog = flushOnErrorLog;
    }

    @Override
    public void append(LogEvent event) {
        if (appenderRefs == null || appenderRefs.length == 0) {
            return;
        }
        LambdaHandlerProcessor.getXrayTraceId().ifPresentOrElse(
                traceId -> {
                    // Check if we should buffer this log level
                    if (shouldBuffer(event.getLevel())) {
                        bufferEvent(traceId, event);
                    } else {
                        callAppenders(event);
                    }

                    // Flush buffer on error logs if configured
                    if (flushOnErrorLog && event.getLevel().isMoreSpecificThan(Level.WARN)) {
                        flushBuffer(traceId);
                    }
                },
                () -> callAppenders(event) // No trace ID (INIT phase), log directly
        );
    }

    private void callAppenders(LogEvent event) {
        for (AppenderRef ref : appenderRefs) {
            Appender appender = configuration.getAppender(ref.getRef());
            if (appender != null) {
                appender.append(event);
            }
        }
    }

    private boolean shouldBuffer(Level level) {
        return level.isLessSpecificThan(bufferAtVerbosity) || level.equals(bufferAtVerbosity);
    }

    private void bufferEvent(String traceId, LogEvent event) {
        // Create immutable copy to prevent mutation
        LogEvent immutableEvent = Log4jLogEvent.createMemento(event);

        // Check if single event is larger than buffer - discard if so
        int eventSize = immutableEvent.getMessage().getFormattedMessage().length();
        if (eventSize > maxBytes) {
            if (Boolean.TRUE != bufferOverflowWarned.get()) {
                bufferOverflowWarned.set(true);
            }
            return;
        }

        bufferCache.computeIfAbsent(traceId, k -> new ArrayDeque<>()).add(immutableEvent);

        // Simple size check - remove oldest if over limit
        Deque<LogEvent> buffer = bufferCache.get(traceId);
        while (getBufferSize(buffer) > maxBytes && !buffer.isEmpty()) {
            if (Boolean.TRUE != bufferOverflowWarned.get()) {
                bufferOverflowWarned.set(true);
            }
            buffer.removeFirst();
        }
    }

    private int getBufferSize(Deque<LogEvent> buffer) {
        return buffer.stream()
                .mapToInt(event -> event.getMessage().getFormattedMessage().length())
                .sum();
    }

    public void clearBuffer() {
        LambdaHandlerProcessor.getXrayTraceId().ifPresent(bufferCache::remove);
    }

    public void flushBuffer() {
        LambdaHandlerProcessor.getXrayTraceId().ifPresent(this::flushBuffer);
    }

    private void flushBuffer(String traceId) {
        Deque<LogEvent> buffer = bufferCache.remove(traceId);
        if (buffer != null) {
            // Emit buffer overflow warning if it occurred
            if (Boolean.TRUE == bufferOverflowWarned.get()) {
                LOGGER.warn("Buffer size exceeded for trace ID: {}. Some log events were discarded.", traceId);
                bufferOverflowWarned.remove();
            }
            buffer.forEach(this::callAppenders);
        }
    }

    @PluginFactory
    public static BufferingAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("AppenderRef") AppenderRef[] appenderRefs,
            @PluginConfiguration Configuration configuration,
            @PluginAttribute(value = "bufferAtVerbosity", defaultString = "DEBUG") String bufferAtVerbosity,
            @PluginAttribute(value = "maxBytes", defaultInt = 20480) int maxBytes,
            @PluginAttribute(value = "flushOnErrorLog", defaultBoolean = true) boolean flushOnErrorLog) {

        if (name == null) {
            LOGGER.error("No name provided for BufferingAppender");
            return null;
        }

        Level level = Level.getLevel(bufferAtVerbosity);
        if (level == null) {
            level = Level.DEBUG;
        }

        return new BufferingAppender(name, filter, layout, appenderRefs, configuration, level, maxBytes,
                flushOnErrorLog);
    }
}
