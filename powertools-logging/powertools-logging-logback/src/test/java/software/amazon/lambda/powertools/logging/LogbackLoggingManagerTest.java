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

package software.amazon.lambda.powertools.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import software.amazon.lambda.powertools.logging.logback.internal.LogbackLoggingManager;

class LogbackLoggingManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackLoggingManagerTest.class);
    private static final Logger ROOT = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @BeforeEach
    void setUp() throws JoranException, IOException {
        resetLogbackConfig("/logback-test.xml");

        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
    }

    @Test
    void getLogLevel_shouldReturnConfiguredLogLevel() {
        LogbackLoggingManager manager = new LogbackLoggingManager();
        Level logLevel = manager.getLogLevel(LOG);
        assertThat(logLevel).isEqualTo(DEBUG);

        logLevel = manager.getLogLevel(ROOT);
        assertThat(logLevel).isEqualTo(WARN);
    }

    @Test
    void resetLogLevel() {
        LogbackLoggingManager manager = new LogbackLoggingManager();
        manager.setLogLevel(ERROR);

        Level logLevel = manager.getLogLevel(LOG);
        assertThat(logLevel).isEqualTo(ERROR);
    }

    @Test
    void shouldDetectMultipleBufferingAppendersRegardlessOfName() throws JoranException {
        // Given - configuration with multiple BufferingAppenders with different names
        resetLogbackConfig("/logback-multiple-buffering.xml");

        Logger logger = LoggerFactory.getLogger("test.multiple.appenders");

        // When - log messages and flush buffers
        logger.debug("Test message 1");
        logger.debug("Test message 2");

        LogbackLoggingManager manager = new LogbackLoggingManager();
        manager.flushBuffer();

        // Then - both appenders should have flushed their buffers
        File logFile = new File("target/logfile.json");
        assertThat(logFile).exists();
        String content = contentOf(logFile);
        // Each message should appear twice (once from each BufferingAppender)
        assertThat(content.split("Test message 1", -1)).hasSize(3); // 2 occurrences = 3 parts
        assertThat(content.split("Test message 2", -1)).hasSize(3); // 2 occurrences = 3 parts
    }

    @AfterEach
    void cleanUp() throws JoranException {
        resetLogbackConfig("/logback-test.xml");
    }

    private void resetLogbackConfig(String configFileName) throws JoranException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(getClass().getResourceAsStream(configFileName));
    }
}
