package software.amazon.lambda.powertools.logging.internal;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.event.Level.*;

public class Log4jLoggingManagerTest {

    private static Logger LOG = LoggerFactory.getLogger(Log4jLoggingManagerTest.class);
    private static Logger ROOT = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Test
    @Order(1)
    public void getLogLevel_shouldReturnConfiguredLogLevel() {
        Log4jLoggingManager manager = new Log4jLoggingManager();
        Level logLevel = manager.getLogLevel(LOG);
        assertThat(logLevel).isEqualTo(INFO);

        logLevel = manager.getLogLevel(ROOT);
        assertThat(logLevel).isEqualTo(WARN);
    }

    @Test
    @Order(2)
    public void resetLogLevel() {
        Log4jLoggingManager manager = new Log4jLoggingManager();
        manager.resetLogLevel(ERROR);

        Level logLevel = manager.getLogLevel(LOG);
        assertThat(logLevel).isEqualTo(ERROR);
    }
}
