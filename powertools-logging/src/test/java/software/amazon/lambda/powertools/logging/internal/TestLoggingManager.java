package software.amazon.lambda.powertools.logging.internal;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.test.TestLogger;
import org.slf4j.test.TestLoggerFactory;

public class TestLoggingManager implements LoggingManager {

    private final TestLoggerFactory loggerFactory;

    public TestLoggingManager() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof TestLoggerFactory)) {
            throw new RuntimeException(
                    "LoggerFactory does not match required type: " + TestLoggerFactory.class.getName());
        }
        this.loggerFactory = (TestLoggerFactory) loggerFactory;
    }

    @Override
    public void setLogLevel(Level logLevel) {
        loggerFactory.getLoggers().forEach((key, logger) -> ((TestLogger) logger).setLogLevel(logLevel.toString()));
    }

    @Override
    public Level getLogLevel(Logger logger) {
        return org.slf4j.event.Level.intToLevel(((TestLogger) logger).getLogLevel());
    }
}
