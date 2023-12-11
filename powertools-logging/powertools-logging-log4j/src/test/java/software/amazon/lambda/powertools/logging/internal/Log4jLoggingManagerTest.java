package software.amazon.lambda.powertools.logging.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import software.amazon.lambda.powertools.logging.log4.internal.Log4jLoggingManager;

class Log4jLoggingManagerTest {

    private final static Logger LOG = LoggerFactory.getLogger(Log4jLoggingManagerTest.class);
    private final static Logger ROOT = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Test
    @Order(1)
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
    @Order(2)
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
}
