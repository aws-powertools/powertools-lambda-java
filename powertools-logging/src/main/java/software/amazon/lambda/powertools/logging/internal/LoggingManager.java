package software.amazon.lambda.powertools.logging.internal;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Due to limitations of SLF4J, we need to rely on implementations for some operations:
 * <ul>
 *     <li>Accessing to all loggers and change their Level</li>
 *     <li>Retrieving the log Level of a Logger</li>
 * </ul>
 *
 * Implementations are provided in submodules and loaded thanks to a {@link java.util.ServiceLoader}
 * (define a file named <code>software.amazon.lambda.powertools.logging.internal.LoggingManager</code> in <code>src/main/resources/META-INF/services</code> with the qualified name of the implementation).
 *
 */
public interface LoggingManager {
    /**
     * Change the log Level of all loggers (named and root)
     * @param logLevel the log Level (slf4j) to apply
     */
    void resetLogLevel(Level logLevel);

    /**
     * Retrieve the log Level of a specific logger
     * @param logger the logger (slf4j) for which to retrieve the log Level
     * @return the Level (slf4j) of this logger. Note that SLF4J only support ERROR, WARN, INFO, DEBUG, TRACE while some frameworks may support others (OFF, FATAL, ...)
     */
    Level getLogLevel(Logger logger);
}
