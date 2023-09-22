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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;

/**
 * LoggingManager for Log4j2 (see {@link LoggingManager}).
 */
public class Log4jLoggingManager implements LoggingManager {

    /**
     * @inheritDoc
     */
    @Override
    @SuppressWarnings("java:S4792")
    public void resetLogLevel(org.slf4j.event.Level logLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.getLevel(logLevel.toString()));
        ctx.updateLoggers();
    }

    /**
     * @inheritDoc
     */
    @Override
    public org.slf4j.event.Level getLogLevel(Logger logger) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        return org.slf4j.event.Level.valueOf(ctx.getLogger(logger.getName()).getLevel().toString());
    }
}
