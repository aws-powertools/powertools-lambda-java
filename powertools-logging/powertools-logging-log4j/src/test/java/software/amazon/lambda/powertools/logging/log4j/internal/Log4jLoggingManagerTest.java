package software.amazon.lambda.powertools.logging.log4j.internal;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

class Log4jLoggingManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(Log4jLoggingManagerTest.class);
    private static final Logger ROOT = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @BeforeEach
    void setUp() {
        // Force reconfiguration from XML to ensure clean state
        Configurator.reconfigure();
    }

    @Test
    void getLogLevel_shouldReturnConfiguredLogLevel() {
        // Given log4j2.xml in resources

        // When
        Log4jLoggingManager manager = new Log4jLoggingManager();
        Level logLevel = manager.getLogLevel(LOG);
        Level rootLevel = manager.getLogLevel(ROOT);

        // Then
        assertThat(logLevel).isEqualTo(DEBUG);
        assertThat(rootLevel).isEqualTo(WARN);
    }

    @Test
    void resetLogLevel() {
        // Given log4j2.xml in resources

        // When
        Log4jLoggingManager manager = new Log4jLoggingManager();
        manager.setLogLevel(ERROR);

        Level rootLevel = manager.getLogLevel(ROOT);
        Level logLevel = manager.getLogLevel(LOG);

        // Then
        assertThat(rootLevel).isEqualTo(ERROR);
        assertThat(logLevel).isEqualTo(ERROR);
    }

    @Test
    void shouldDetectMultipleBufferingAppendersRegardlessOfName() throws IOException {
        // Given - configuration with multiple BufferingAppenders with different names
        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration originalConfig = ctx.getConfiguration();

        try {
            ConfigurationFactory factory = new XmlConfigurationFactory();
            ConfigurationSource source = new ConfigurationSource(
                    getClass().getResourceAsStream("/log4j2-multiple-buffering.xml"));
            Configuration config = factory.getConfiguration(null, source);

            ctx.setConfiguration(config);
            ctx.updateLoggers();

            org.apache.logging.log4j.Logger logger = LogManager.getLogger("test.multiple.appenders");

            // When - log messages and flush buffers
            logger.debug("Test message 1");
            logger.debug("Test message 2");

            Log4jLoggingManager manager = new Log4jLoggingManager();
            manager.flushBuffer();

            // Then - both appenders should have flushed their buffers
            File logFile = new File("target/logfile.json");
            assertThat(logFile).exists();
            String content = contentOf(logFile);
            // Each message should appear twice (once from each BufferingAppender)
            assertThat(content.split("Test message 1", -1)).hasSize(3); // 2 occurrences = 3 parts
            assertThat(content.split("Test message 2", -1)).hasSize(3); // 2 occurrences = 3 parts
        } finally {
            // Restore original configuration to prevent test interference
            ctx.setConfiguration(originalConfig);
            ctx.updateLoggers();
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        // Reset to original configuration from XML
        Configurator.reconfigure();

        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there
        }
    }
}
