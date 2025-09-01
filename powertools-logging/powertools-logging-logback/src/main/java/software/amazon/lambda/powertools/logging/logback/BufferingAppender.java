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

package software.amazon.lambda.powertools.logging.logback;

import java.util.Deque;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor;
import software.amazon.lambda.powertools.logging.internal.BufferManager;
import software.amazon.lambda.powertools.logging.internal.KeyBuffer;

/**
 * A Logback appender that buffers log events by AWS X-Ray trace ID for optimized Lambda logging.
 * 
 * <p>This appender is designed specifically for AWS Lambda functions to reduce log ingestion
 * by buffering lower-level logs and only outputting them when errors occur, preserving
 * full context for troubleshooting while minimizing routine log volume.
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Trace-based buffering:</strong> Groups logs by AWS X-Ray trace ID</li>
 *   <li><strong>Selective output:</strong> Only buffers logs at or below configured verbosity level</li>
 *   <li><strong>Auto-flush on errors:</strong> Automatically outputs buffered logs when ERROR events occur</li>
 *   <li><strong>Memory management:</strong> Prevents memory leaks with configurable buffer size limits</li>
 *   <li><strong>Overflow protection:</strong> Warns when logs are discarded due to buffer limits</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * <appender name="BufferedAppender" class="software.amazon.lambda.powertools.logging.logback.BufferingAppender">
 *   <bufferAtVerbosity>DEBUG</bufferAtVerbosity>
 *   <maxBytes>20480</maxBytes>
 *   <flushOnErrorLog>true</flushOnErrorLog>
 *   <appender-ref ref="ConsoleAppender"/>
 * </appender>
 * }</pre>
 * 
 * <h3>Configuration Parameters:</h3>
 * <ul>
 *   <li><strong>bufferAtVerbosity:</strong> Log level to buffer (default: DEBUG). Logs at this level and below are buffered</li>
 *   <li><strong>maxBytes:</strong> Maximum buffer size in bytes per trace ID (default: 20480)</li>
 *   <li><strong>flushOnErrorLog:</strong> Whether to flush buffer on ERROR logs (default: true)</li>
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
public class BufferingAppender extends AppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent>, BufferManager {

    private static final int DEFAULT_BUFFER_SIZE = 20480;

    private final AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<>();
    private Level bufferAtVerbosity = Level.DEBUG;
    private boolean flushOnErrorLog = true;
    private int maxBytes = DEFAULT_BUFFER_SIZE;
    private KeyBuffer<String, ILoggingEvent> buffer;

    @Override
    public void start() {
        // Initialize lazily to ensure configuration properties are set first.
        if (buffer == null) {
            buffer = new KeyBuffer<>(maxBytes, event -> event.getFormattedMessage().length(), this::logOverflowWarning);
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        LambdaHandlerProcessor.getXrayTraceId().ifPresentOrElse(
                traceId -> {
                    if (shouldBuffer(event.getLevel())) {
                        buffer.add(traceId, event);
                    } else {
                        aai.appendLoopOnAppenders(event);
                    }

                    // Flush buffer on error logs if configured
                    if (flushOnErrorLog && event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                        flushBuffer(traceId);
                    }
                },
                () -> aai.appendLoopOnAppenders(event) // No trace ID (INIT phase), log directly
        );
    }

    private boolean shouldBuffer(Level level) {
        return level.levelInt <= bufferAtVerbosity.levelInt;
    }

    public void clearBuffer() {
        LambdaHandlerProcessor.getXrayTraceId().ifPresent(buffer::clear);
    }

    public void flushBuffer() {
        LambdaHandlerProcessor.getXrayTraceId().ifPresent(this::flushBuffer);
    }

    private void flushBuffer(String traceId) {
        Deque<ILoggingEvent> events = buffer.removeAll(traceId);
        if (events != null) {
            events.forEach(aai::appendLoopOnAppenders);
        }
    }

    // Configuration setters. These will be inspected as JavaBean properties by Logback
    // when configuring the appender via XML or programmatically. They run before start().
    public void setBufferAtVerbosity(String level) {
        this.bufferAtVerbosity = Level.toLevel(level, Level.DEBUG);
    }

    public void setMaxBytes(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    public void setFlushOnErrorLog(boolean flushOnErrorLog) {
        this.flushOnErrorLog = flushOnErrorLog;
    }

    // AppenderAttachable implementation. We simply delegate to the internal logback AppenderAttachableImpl. This is
    // needed to be able to attach other appenders to this appender so that customers can wrap existing appenders with
    // this buffering appender.
    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        aai.addAppender(newAppender);
    }

    @Override
    public java.util.Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }

    private void logOverflowWarning() {
        // Create a properly formatted warning event and send directly to child appenders. Used to avoid circular
        // dependency between KeyBuffer and BufferingAppender.
        Logger logbackLogger = (Logger) org.slf4j.LoggerFactory
                .getLogger(BufferingAppender.class);
        LoggingEvent warningEvent = new LoggingEvent(
                BufferingAppender.class.getName(), logbackLogger, Level.WARN,
                "Some logs are not displayed because they were evicted from the buffer. Increase buffer size to store more logs in the buffer.",
                null, null);
        warningEvent.setTimeStamp(System.currentTimeMillis());
        aai.appendLoopOnAppenders(warningEvent);
    }
}
