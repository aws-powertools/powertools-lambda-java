package software.amazon.lambda.logging;

import org.apache.logging.log4j.ThreadContext;

public class PowerLogger {

    public static void customKey(String key, String value) {
        ThreadContext.put(key, value);
    }
}
