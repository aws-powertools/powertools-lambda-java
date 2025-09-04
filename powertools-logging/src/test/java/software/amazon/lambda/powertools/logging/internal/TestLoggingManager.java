package software.amazon.lambda.powertools.logging.internal;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.test.TestLogger;
import org.slf4j.test.TestLoggerFactory;

public class TestLoggingManager implements LoggingManager, BufferManager {

    private final TestLoggerFactory loggerFactory;
    private boolean bufferFlushed = false;
    private boolean bufferCleared = false;

    public TestLoggingManager() {
        ILoggerFactory loggerFactoryInstance = LoggerFactory.getILoggerFactory();
        if (!(loggerFactoryInstance instanceof TestLoggerFactory)) {
            throw new RuntimeException(
                    "LoggerFactory does not match required type: " + TestLoggerFactory.class.getName());
        }
        this.loggerFactory = (TestLoggerFactory) loggerFactoryInstance;
    }

    @Override
    public void setLogLevel(Level logLevel) {
        loggerFactory.getLoggers().forEach((key, logger) -> ((TestLogger) logger).setLogLevel(logLevel.toString()));
    }

    @Override
    public Level getLogLevel(Logger logger) {
        return org.slf4j.event.Level.intToLevel(((TestLogger) logger).getLogLevel());
    }

    @Override
    public void flushBuffer() {
        bufferFlushed = true;
    }

    @Override
    public void clearBuffer() {
        bufferCleared = true;
    }

    public boolean isBufferFlushed() {
        return bufferFlushed;
    }

    public boolean isBufferCleared() {
        return bufferCleared;
    }

    public void resetBufferState() {
        bufferFlushed = false;
        bufferCleared = false;
    }
}
