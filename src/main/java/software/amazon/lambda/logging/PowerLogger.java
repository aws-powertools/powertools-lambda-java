package software.amazon.lambda.logging;

import org.apache.logging.log4j.ThreadContext;

/**
 * A class of helper functions to add additional functionality to PowerToolsLogging.
 *
 * {@see PowerToolsLogging}
 */
public class PowerLogger {

    /**
     * Appends an additional key and value to each log entry made. Duplicate values
     * for the same key will be replaced with the latest.
     *
     * @param key The name of the key to be logged
     * @param value The value to be logged
     */
    public static void appendKey(String key, String value) {
        ThreadContext.put(key, value);
    }
}
