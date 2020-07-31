package software.aws.lambda.logging.client;

import org.apache.logging.log4j.ThreadContext;

public class PowerLogger {

    public static void customKey(String key, String value) {
        ThreadContext.put(key, value);
    }
}
