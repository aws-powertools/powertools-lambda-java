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

import static software.amazon.lambda.powertools.logging.log4j.Log4jConstants.BUFFERING_APPENDER_PLUGIN_NAME;

import java.io.Serializable;
import java.util.Deque;

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
import org.apache.logging.log4j.message.SimpleMessage;

import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.BufferManager;
import software.amazon.lambda.powertools.logging.internal.KeyBuffer;

/**
 * A Log4j2 appender that buffers log events by AWS X-Ray trace ID for optimized Lambda logging.
 * 
 * <p>This appender is designed specifically for AWS Lambda functions to reduce log ingestion
 * by buffering lower-level logs and only outputting them when errors occur, preserving
 * full context for troubleshooting while minimizing routine log volume.
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Trace-based buffering:</strong> Groups logs by AWS X-Ray trace ID</li>
 *   <li><strong>Selective output:</strong> Only buffers logs at or below configured verbosity level</li>
 *   <li><strong>Auto-flush on errors:</strong> Automatically outputs buffered logs when ERROR/FATAL events occur</li>
 *   <li><strong>Memory management:</strong> Prevents memory leaks with configurable buffer size limits</li>
 *   <li><strong>Overflow protection:</strong> Warns when logs are discarded due to buffer limits</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * <BufferingAppender name="BufferedAppender" 
 *                    bufferAtVerbosity="DEBUG" 
 *                    maxBytes="20480" 
 *                    flushOnErrorLog="true">
 *   <AppenderRef ref="ConsoleAppender"/>
 * </BufferingAppender>
 * }</pre>
 * 
 * <h3>Configuration Parameters:</h3>
 * <ul>
 *   <li><strong>bufferAtVerbosity:</strong> Log level to buffer (default: DEBUG). Logs at this level and below are buffered</li>
 *   <li><strong>maxBytes:</strong> Maximum buffer size in bytes per trace ID (default: 20480)</li>
 *   <li><strong>flushOnErrorLog:</strong> Whether to flush buffer on ERROR/FATAL logs (default: true)</li>
 * </ul>
 * 
 * <h3>Behavior:</h3>
 * <ul>
 *   <li>During Lambda INIT phase (no trace ID): logs are output directly</li>
 *   <li>During Lambda execution (with trace ID): logs are buffered or output based on level</li>
 *   <li>When buffer overflows: oldest logs are discarded and a warning is logged</li>
 *   <li>On Lambda completion: remaining buffered logs can be flushed via {@link software.amazon.lambda.powertools.logging.PowertoolsLogging}</li>
 * </ul>
 * 
 * @see software.amazon.lambda.powertools.logging.PowertoolsLogging#flushBuffer()
 */
@Plugin(name = BUFFERING_APPENDER_PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class BufferingAppender extends AbstractAppender implements BufferManager {

    private final AppenderRef[] appenderRefs;
    private final Configuration configuration;
    private final Level bufferAtVerbosity;
    private final boolean flushOnErrorLog;
    private final KeyBuffer<String, LogEvent> buffer;

    @SuppressWarnings("java:S107") // Constructor has too many parameters, which is OK for a Log4j2 plugin
    protected BufferingAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            AppenderRef[] appenderRefs, Configuration configuration, Level bufferAtVerbosity, int maxBytes,
            boolean flushOnErrorLog) {
        super(name, filter, layout, false, null);
        this.appenderRefs = appenderRefs;
        this.configuration = configuration;
        this.bufferAtVerbosity = bufferAtVerbosity;
        this.flushOnErrorLog = flushOnErrorLog;
        this.buffer = new KeyBuffer<>(maxBytes, event -> event.getMessage().getFormattedMessage().length(),
                this::logOverflowWarning);
    }

    @Override
    public void append(LogEvent event) {
        if (appenderRefs == null || appenderRefs.length == 0) {
            return;
        }

        LambdaHandlerProcessor.getXrayTraceId().ifPresentOrElse(
                traceId -> {
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
        LogEvent immutableEvent = Log4jLogEvent.createMemento(event);
        buffer.add(traceId, immutableEvent);
    }

    public void clearBuffer() {
        LambdaHandlerProcessor.getXrayTraceId().ifPresent(buffer::clear);
    }

    public void flushBuffer() {
        LambdaHandlerProcessor.getXrayTraceId().ifPresent(this::flushBuffer);
    }

    private void flushBuffer(String traceId) {
        Deque<LogEvent> events = buffer.removeAll(traceId);
        if (events != null) {
            events.forEach(this::callAppenders);
        }
    }

    @PluginFactory
    @SuppressWarnings("java:S107") // Method has too many parameters, which is OK for a Log4j2 plugin factory
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

    private void logOverflowWarning() {
        // Create a properly formatted warning event and send directly to child appenders. Used to avoid circular
        // dependency between KeyBuffer and BufferingAppender.
        SimpleMessage message = new SimpleMessage(
                "Some logs are not displayed because they were evicted from the buffer. Increase buffer size to store more logs in the buffer.");
        LogEvent warningEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(BufferingAppender.class.getName())
                .setLevel(Level.WARN)
                .setMessage(message)
                .setTimeMillis(System.currentTimeMillis())
                .build();
        callAppenders(warningEvent);
    }
}
