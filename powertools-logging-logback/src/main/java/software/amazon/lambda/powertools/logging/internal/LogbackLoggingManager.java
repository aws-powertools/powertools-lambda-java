package software.amazon.lambda.powertools.logging.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.List;

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
