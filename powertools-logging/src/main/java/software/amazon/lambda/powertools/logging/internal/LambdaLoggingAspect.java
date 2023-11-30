/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.logging.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.logging.LoggingUtils.appendKey;
import static software.amazon.lambda.powertools.logging.LoggingUtils.appendKeys;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_EVENT;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_LEVEL;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_SAMPLING_RATE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SERVICE;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.LAMBDA_LOG_LEVEL;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.burt.jmespath.Expression;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.ServiceLoader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.amazon.lambda.powertools.utilities.JsonConfig;


@Aspect
@DeclarePrecedence("*, software.amazon.lambda.powertools.logging.internal.LambdaLoggingAspect")
public final class LambdaLoggingAspect {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaLoggingAspect.class);
    private static final Random SAMPLER = new Random();
    static Level LEVEL_AT_INITIALISATION; /* not final for test purpose */

    private static final LoggingManager LOGGING_MANAGER;

    static {
        LOGGING_MANAGER = getLoggingManagerFromServiceLoader();

        setLogLevel();

        LEVEL_AT_INITIALISATION = LOGGING_MANAGER.getLogLevel(LOG);
    }

    static void setLogLevel() {
        if (POWERTOOLS_LOG_LEVEL != null) {
            Level powertoolsLevel = getLevelFromEnvironmentVariable(POWERTOOLS_LOG_LEVEL);
            if (LAMBDA_LOG_LEVEL != null) {
                Level lambdaLevel = getLevelFromEnvironmentVariable(LAMBDA_LOG_LEVEL);
                if (powertoolsLevel.toInt() < lambdaLevel.toInt()) {
                    LOG.warn("Current log level ({}) does not match AWS Lambda Advanced Logging Controls minimum log level ({}). This can lead to data loss, consider adjusting them.",
                            POWERTOOLS_LOG_LEVEL, LAMBDA_LOG_LEVEL);
                }
            }
            resetLogLevels(powertoolsLevel);
        } else if (LAMBDA_LOG_LEVEL != null) {
            resetLogLevels(getLevelFromEnvironmentVariable(LAMBDA_LOG_LEVEL));
        }
    }

    private static Level getLevelFromEnvironmentVariable(String level) {
        if (Arrays.stream(Level.values()).anyMatch(slf4jLevel -> slf4jLevel.name().equalsIgnoreCase(level))) {
            return Level.valueOf(level.toUpperCase());
        } else {
            // FATAL does not exist in slf4j
            if ("FATAL".equalsIgnoreCase(level)) {
                return Level.ERROR;
            }
        }
        // default to INFO if incorrect value
        return Level.INFO;
    }

    /**
     * Use {@link ServiceLoader} to lookup for a {@link LoggingManager}.
     * A file <i>software.amazon.lambda.powertools.logging.internal.LoggingManager</i> must be created in
     * <i>META-INF/services/</i> folder with the appropriate implementation of the {@link LoggingManager}
     *
     * @return an instance of {@link LoggingManager}
     * @throws IllegalStateException if no {@link LoggingManager} could be found
     */
    private static LoggingManager getLoggingManagerFromServiceLoader() {
        LoggingManager loggingManager;

        ServiceLoader<LoggingManager> loggingManagers = ServiceLoader.load(LoggingManager.class);
        List<LoggingManager> loggingManagerList = new ArrayList<>();
        for (LoggingManager lm : loggingManagers) {
            loggingManagerList.add(lm);
        }
        if (loggingManagerList.isEmpty()) {
            throw new IllegalStateException("No LoggingManager was found on the classpath, "
                    +
                    "make sure to add either powertools-logging-log4j or powertools-logging-logback to your dependencies");
        } else if (loggingManagerList.size() > 1) {
            throw new IllegalStateException(
                    "Multiple LoggingManagers were found on the classpath: " + loggingManagerList
                            +
                            ", make sure to have only one of powertools-logging-log4j OR powertools-logging-logback to your dependencies");
        } else {
            loggingManager = loggingManagerList.get(0);
        }
        return loggingManager;
    }

    private static void resetLogLevels(Level logLevel) {
        LOGGING_MANAGER.resetLogLevel(logLevel);
    }

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(logging)")
    public void callAt(Logging logging) {
    }

    /**
     * Main method of the aspect
     */
    @Around(value = "callAt(logging) && execution(@Logging * *.*(..))", argNames = "pjp,logging")
    public Object around(ProceedingJoinPoint pjp,
                         Logging logging) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        setLogLevelBasedOnSamplingRate(pjp, logging);

        Context extractedContext = extractContext(pjp);

        if (null != extractedContext) {
            appendKeys(PowertoolsLoggedFields.setValuesFromLambdaContext(extractedContext));
            appendKey(FUNCTION_COLD_START.getName(), isColdStart() ? "true" : "false");
            appendKey(SERVICE.getName(), serviceName());
        }

        getXrayTraceId().ifPresent(xRayTraceId -> appendKey(FUNCTION_TRACE_ID.getName(), xRayTraceId));

        if (logging.logEvent() || "true".equals(POWERTOOLS_LOG_EVENT)) {
            proceedArgs = logEvent(pjp);
        }

        if (!logging.correlationIdPath().isEmpty()) {
            proceedArgs = captureCorrelationId(logging.correlationIdPath(), pjp);
        }

        Object proceed = pjp.proceed(proceedArgs);

        if (logging.clearState()) {
            MDC.clear();
        }

        coldStartDone();
        return proceed;
    }

    private void setLogLevelBasedOnSamplingRate(final ProceedingJoinPoint pjp,
                                                final Logging logging) {
        double samplingRate = samplingRate(logging);

        if (isHandlerMethod(pjp)) {

            if (samplingRate < 0 || samplingRate > 1) {
                LOG.warn("Skipping sampling rate configuration because of invalid value. Sampling rate: {}",
                        samplingRate);
                return;
            }

            appendKey(PowertoolsLoggedFields.SAMPLING_RATE.getName(), String.valueOf(samplingRate));

            if (samplingRate == 0) {
                return;
            }

            float sample = SAMPLER.nextFloat();

            if (samplingRate > sample) {
                resetLogLevels(Level.DEBUG);

                LOG.debug("Changed log level to DEBUG based on Sampling configuration. "
                        + "Sampling Rate: {}, Sampler Value: {}.", samplingRate, sample);
            } else if (LEVEL_AT_INITIALISATION != LOGGING_MANAGER.getLogLevel(LOG)) {
                resetLogLevels(LEVEL_AT_INITIALISATION);
            }
        }
    }

    private double samplingRate(final Logging logging) {
        String sampleRate = POWERTOOLS_SAMPLING_RATE;
        if (null != sampleRate) {
            try {
                return Double.parseDouble(sampleRate);
            } catch (NumberFormatException e) {
                LOG.warn("Skipping sampling rate on environment variable configuration because of invalid "
                        + "value. Sampling rate: {}", sampleRate);
            }
        }
        return logging.samplingRate();
    }

    private Object[] logEvent(final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        LoggingUtils.logMessagesAsJson(true);

        if (placedOnRequestHandler(pjp)) {
            Logger log = logger(pjp);
            asJson(pjp, pjp.getArgs()[0])
                    .ifPresent(log::info);
        } else if (placedOnStreamHandler(pjp)) {
            args = logFromInputStream(pjp);
        }

        LoggingUtils.logMessagesAsJson(false);
        return args;
    }

    private Object[] captureCorrelationId(final String correlationIdPath,
                                          final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        if (placedOnRequestHandler(pjp)) {
            Object arg = pjp.getArgs()[0];
            JsonNode jsonNode = JsonConfig.get().getObjectMapper().valueToTree(arg);

            setCorrelationIdFromNode(correlationIdPath, pjp, jsonNode);

            return args;
        } else if (placedOnStreamHandler(pjp)) {
            try {
                byte[] bytes = bytesFromInputStreamSafely((InputStream) pjp.getArgs()[0]);
                JsonNode jsonNode = JsonConfig.get().getObjectMapper().readTree(bytes);
                args[0] = new ByteArrayInputStream(bytes);

                setCorrelationIdFromNode(correlationIdPath, pjp, jsonNode);

                return args;
            } catch (IOException e) {
                Logger log = logger(pjp);
                log.warn("Failed to capture correlation id on event from supplied input stream.", e);
            }
        }

        return args;
    }

    private void setCorrelationIdFromNode(String correlationIdPath, ProceedingJoinPoint pjp, JsonNode jsonNode) {
        Expression<JsonNode> jmesExpression = JsonConfig.get().getJmesPath().compile(correlationIdPath);
        JsonNode node = jmesExpression.search(jsonNode);

        String asText = node.asText();
        if (null != asText && !asText.isEmpty()) {
            LoggingUtils.setCorrelationId(asText);
        } else {
            logger(pjp).warn("Unable to extract any correlation id. Is your function expecting supported event type?");
        }
    }

    private Object[] logFromInputStream(final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        try {
            byte[] bytes = bytesFromInputStreamSafely((InputStream) pjp.getArgs()[0]);
            args[0] = new ByteArrayInputStream(bytes);
            Logger log = logger(pjp);

            asJson(pjp, JsonConfig.get().getObjectMapper().readValue(bytes, Map.class))
                    .ifPresent(log::info);

        } catch (IOException e) {
            Logger log = logger(pjp);
            log.warn("Failed to log event from supplied input stream.", e);
        }

        return args;
    }

    private byte[] bytesFromInputStreamSafely(final InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStreamReader reader = new InputStreamReader(inputStream, UTF_8)) {
            OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
            int n;
            char[] buffer = new char[4096];
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }
            writer.flush();
            return out.toByteArray();
        }
    }

    private Optional<String> asJson(final ProceedingJoinPoint pjp,
                                    final Object target) {
        try {
            return ofNullable(JsonConfig.get().getObjectMapper().writeValueAsString(target));
        } catch (JsonProcessingException e) {
            logger(pjp).error("Failed logging event of type {}", target.getClass(), e);
            return empty();
        }
    }

    private Logger logger(final ProceedingJoinPoint pjp) {
        return LoggerFactory.getLogger(pjp.getSignature().getDeclaringType());
    }
}
