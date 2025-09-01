package software.amazon.lambda.powertools.logging.log4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;

class BufferingAppenderTest {

    private Logger logger;

    @BeforeEach
    void setUp() throws IOException {
        logger = LogManager.getLogger(BufferingAppenderTest.class);

        try {
            FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
        } catch (NoSuchFileException e) {
            // may not be there in the first run
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileChannel.open(Paths.get("target/logfile.json"), StandardOpenOption.WRITE).truncate(0).close();
    }

    @Test
    void shouldBufferDebugLogsAndFlushOnError() {
        // When - log debug messages (should be buffered)
        logger.debug("Debug message 1");
        logger.debug("Debug message 2");

        // Then - no logs written yet
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).isEmpty();

        // When - log error (should flush buffer)
        logger.error("Error message");

        // Then - all logs written
        assertThat(contentOf(logFile))
                .contains("Debug message 1")
                .contains("Debug message 2")
                .contains("Error message");
    }

    @Test
    @ClearEnvironmentVariable(key = "_X_AMZN_TRACE_ID")
    void shouldLogDirectlyWhenNoTraceId() {
        // When
        logger.debug("Debug without trace");

        // Then - log written directly
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("Debug without trace");
    }

    @Test
    void shouldNotBufferInfoLogs() {
        // When - log info message (above buffer level)
        logger.info("Info message");

        // Then - log written directly
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).contains("Info message");
    }

    @Test
    void shouldFlushBufferManually() {
        // When - buffer debug logs
        logger.debug("Buffered message");
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile)).isEmpty();

        // When - manual flush
        BufferingAppender appender = getBufferingAppender();
        appender.flushBuffer();

        // Then - logs written
        assertThat(contentOf(logFile)).contains("Buffered message");
    }

    @Test
    void shouldClearBufferManually() {
        // When - buffer debug logs then clear
        logger.debug("Buffered message");
        BufferingAppender appender = getBufferingAppender();
        appender.clearBuffer();

        // When - log error (should not flush cleared buffer)
        logger.error("Error after clear");

        // Then - only error logged
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains("Error after clear")
                .doesNotContain("Buffered message");
    }

    @Test
    void shouldLogOverflowWarningWhenBufferOverflows() {
        // When - fill buffer beyond capacity to trigger overflow
        for (int i = 0; i < 100; i++) {
            logger.debug("Debug message " + i);
        }

        // When - flush buffer to trigger overflow warning
        BufferingAppender appender = getBufferingAppender();
        appender.flushBuffer();

        // Then - overflow warning should be logged
        File logFile = new File("target/logfile.json");
        assertThat(contentOf(logFile))
                .contains("Some logs are not displayed because they were evicted from the buffer");
    }

    private BufferingAppender getBufferingAppender() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Appender appender = context.getConfiguration().getAppender("BufferingAppender");
        if (appender == null) {
            throw new IllegalStateException("BufferingAppender not found in configuration. Available appenders: " +
                    context.getConfiguration().getAppenders().keySet());
        }
        return (BufferingAppender) appender;
    }
}
