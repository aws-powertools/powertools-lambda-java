package software.amazon.lambda.logging.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.IOUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.logging.PowerToolsLogging;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.internal.LambdaHandlerProcessor.serviceName;

@Aspect
public final class LambdaLoggingAspect {
    private static String LOG_LEVEL = System.getenv("LOG_LEVEL");

    static {
        if (LOG_LEVEL != null) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.getLevel(LOG_LEVEL));
            ctx.updateLoggers();
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    @Pointcut("@annotation(powerToolsLogging)")
    public void callAt(PowerToolsLogging powerToolsLogging) {
    }

    @Around(value = "callAt(powerToolsLogging) && execution(@PowerToolsLogging * *.*(..))", argNames = "pjp,powerToolsLogging")
    public Object around(ProceedingJoinPoint pjp,
                         PowerToolsLogging powerToolsLogging) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        extractContext(pjp)
                .ifPresent(context -> {
                    ThreadContext.putAll(DefaultLambdaFields.values(context));
                    ThreadContext.put("coldStart", null == isColdStart() ? "true" : "false");
                    ThreadContext.put("service", serviceName());
                });


        if (powerToolsLogging.logEvent()) {
            proceedArgs = logEvent(pjp);
        }

        Object proceed = pjp.proceed(proceedArgs);

        coldStartDone();
        return proceed;
    }

    private Optional<Context> extractContext(ProceedingJoinPoint pjp) {

        if (isHandlerMethod(pjp)) {
            if (placedOnRequestHandler(pjp)) {
                return of((Context) pjp.getArgs()[1]);
            }

            if (placedOnStreamHandler(pjp)) {
                return of((Context) pjp.getArgs()[2]);
            }
        }

        return empty();
    }

    private Object[] logEvent(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        if (isHandlerMethod(pjp)) {
            if (placedOnRequestHandler(pjp)) {
                Logger log = logger(pjp);
                log.info(pjp.getArgs()[0]);
            }

            if (placedOnStreamHandler(pjp)) {
                args = logFromInputStream(pjp);
            }
        }

        return args;
    }

    private Object[] logFromInputStream(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out);
             InputStreamReader reader = new InputStreamReader((InputStream) pjp.getArgs()[0])) {

            IOUtils.copy(reader, writer);
            writer.flush();
            byte[] bytes = out.toByteArray();
            args[0] = new ByteArrayInputStream(bytes);

            Logger log = logger(pjp);
            log.info(mapper.readValue(bytes, Map.class));

        } catch (IOException e) {
            Logger log = logger(pjp);
            log.debug("Failed to log event from supplied input stream.", e);
        }

        return args;
    }

    private Logger logger(ProceedingJoinPoint pjp) {
        return LogManager.getLogger(pjp.getSignature().getDeclaringType());
    }
}
