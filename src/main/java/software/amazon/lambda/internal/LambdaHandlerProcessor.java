package software.amazon.lambda.internal;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.aspectj.lang.ProceedingJoinPoint;

public final class LambdaHandlerProcessor {
    // Purposefully not final to enable testing
    private static String SERVICE_NAME = null != System.getenv("POWERTOOLS_SERVICE_NAME")
            ? System.getenv("POWERTOOLS_SERVICE_NAME") : "service_undefined";
    private static Boolean IS_COLD_START = null;

    public static boolean isHandlerMethod(ProceedingJoinPoint pjp) {
        return "handleRequest".equals(pjp.getSignature().getName());
    }

    public static boolean placedOnRequestHandler(ProceedingJoinPoint pjp) {
        return RequestHandler.class.isAssignableFrom(pjp.getSignature().getDeclaringType())
                && pjp.getArgs().length == 2
                && pjp.getArgs()[1] instanceof Context;
    }

    public static boolean placedOnStreamHandler(ProceedingJoinPoint pjp) {
        return RequestStreamHandler.class.isAssignableFrom(pjp.getSignature().getDeclaringType())
                && pjp.getArgs().length == 3
                && pjp.getArgs()[0] instanceof InputStream
                && pjp.getArgs()[1] instanceof OutputStream
                && pjp.getArgs()[2] instanceof Context;
    }

    public static String serviceName() {
        return SERVICE_NAME;
    }

    public static Boolean isColdStart() {
        return IS_COLD_START;
    }

    public static void coldStartDone() {
        IS_COLD_START = false;
    }
}
