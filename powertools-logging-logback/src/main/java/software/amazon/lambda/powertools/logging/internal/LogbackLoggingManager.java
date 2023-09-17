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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.List;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class LogbackLoggingManager implements LoggingManager {

    private final LoggerContext loggerContext;

    public LogbackLoggingManager() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext)) {
            throw new RuntimeException("LoggerFactory does not match required type: " + LoggerContext.class.getName());
        }
        loggerContext = (LoggerContext) loggerFactory;
    }

    @Override
    public void resetLogLevel(org.slf4j.event.Level logLevel) {
        List<Logger> loggers = loggerContext.getLoggerList();
        for (Logger logger : loggers) {
            logger.setLevel(Level.convertAnSLF4JLevel(logLevel));
        }
    }

    @Override
    public org.slf4j.event.Level getLogLevel(org.slf4j.Logger logger) {
        return org.slf4j.event.Level.valueOf(loggerContext.getLogger(logger.getName()).getEffectiveLevel().toString());
    }
}
