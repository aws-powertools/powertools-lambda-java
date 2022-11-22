package software.amazon.lambda.powertools.logging.internal;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;

public class Log4jLoggingManager implements LoggingManager {

    @Override
    public void resetLogLevel(org.slf4j.event.Level logLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.getLevel(logLevel.toString()));
        ctx.updateLoggers();
    }

    @Override
    public org.slf4j.event.Level getLogLevel(Logger logger) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        return org.slf4j.event.Level.valueOf(ctx.getLogger(logger.getName()).getLevel().toString());
    }


}
