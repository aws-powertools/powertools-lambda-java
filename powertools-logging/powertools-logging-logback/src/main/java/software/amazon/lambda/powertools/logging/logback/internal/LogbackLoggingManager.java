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

package software.amazon.lambda.powertools.logging.logback.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import software.amazon.lambda.powertools.logging.internal.BufferManager;
import software.amazon.lambda.powertools.logging.internal.LoggingManager;
import software.amazon.lambda.powertools.logging.logback.BufferingAppender;

/**
 * LoggingManager for Logback that provides log level management and buffer operations.
 * Implements both {@link LoggingManager} and {@link BufferManager} interfaces.
 */
public class LogbackLoggingManager implements LoggingManager, BufferManager {

    private final LoggerContext loggerContext;

    public LogbackLoggingManager() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext)) {
            throw new RuntimeException("LoggerFactory does not match required type: " + LoggerContext.class.getName());
        }
        loggerContext = (LoggerContext) loggerFactory;
    }

    /**
     * @inheritDoc
     */
    @Override
    @SuppressWarnings("java:S4792")
    public void setLogLevel(org.slf4j.event.Level logLevel) {
        List<Logger> loggers = loggerContext.getLoggerList();
        for (Logger logger : loggers) {
            logger.setLevel(Level.convertAnSLF4JLevel(logLevel));
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public org.slf4j.event.Level getLogLevel(org.slf4j.Logger logger) {
        return org.slf4j.event.Level.valueOf(loggerContext.getLogger(logger.getName()).getEffectiveLevel().toString());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void flushBuffer() {
        getBufferingAppenders().forEach(BufferingAppender::flushBuffer);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void clearBuffer() {
        getBufferingAppenders().forEach(BufferingAppender::clearBuffer);
    }

    private Collection<BufferingAppender> getBufferingAppenders() {
        // Search all buffering appenders to avoid relying on the appender name given by the user
        return loggerContext.getLoggerList().stream()
                .flatMap(logger -> {
                    Iterator<Appender<ILoggingEvent>> iterator = logger.iteratorForAppenders();
                    List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
                    iterator.forEachRemaining(appenders::add);
                    return appenders.stream();
                })
                .filter(BufferingAppender.class::isInstance)
                .map(BufferingAppender.class::cast)
                .collect(Collectors.toList());
    }
}
