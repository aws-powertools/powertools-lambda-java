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
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.LAMBDA_LOG_LEVEL;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_ERROR;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_EVENT;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_LEVEL;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_RESPONSE;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_SAMPLING_RATE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.CORRELATION_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SERVICE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;

import io.burt.jmespath.Expression;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.utilities.JsonConfig;

@Aspect
@DeclarePrecedence("*, software.amazon.lambda.powertools.logging.internal.LambdaLoggingAspect")
public final class LambdaLoggingAspect {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaLoggingAspect.class);
    private static final Random SAMPLER = new Random();
    private static Level LEVEL_AT_INITIALISATION; /* not final for test purpose */

    private static final LoggingManager LOGGING_MANAGER;

    static {
        LOGGING_MANAGER = LoggingManagerRegistry.getLoggingManager();

        setLogLevel();

        LEVEL_AT_INITIALISATION = LOGGING_MANAGER.getLogLevel(LOG);
    }

    static void setLogLevel() {
        if (POWERTOOLS_LOG_LEVEL != null) {
            Level powertoolsLevel = getLevelFromString(POWERTOOLS_LOG_LEVEL);
            if (LAMBDA_LOG_LEVEL != null) {
                Level lambdaLevel = getLevelFromString(LAMBDA_LOG_LEVEL);
                if (powertoolsLevel.toInt() < lambdaLevel.toInt()) {
                    LOG.warn(
                            "Current log level ({}) does not match AWS Lambda Advanced Logging Controls minimum log level ({}). This can lead to data loss, consider adjusting them.",
                            POWERTOOLS_LOG_LEVEL, LAMBDA_LOG_LEVEL);
                }
            }
            setLogLevels(powertoolsLevel);
        } else if (LAMBDA_LOG_LEVEL != null) {
            setLogLevels(getLevelFromString(LAMBDA_LOG_LEVEL));
        }
    }

    private static Level getLevelFromString(String level) {
        if (Arrays.stream(Level.values()).anyMatch(slf4jLevel -> slf4jLevel.name().equalsIgnoreCase(level))) {
            return Level.valueOf(level.toUpperCase(Locale.ROOT));
        } else {
            // FATAL does not exist in slf4j
            if ("FATAL".equalsIgnoreCase(level)) {
                return Level.ERROR;
            }
        }
        // default to INFO if incorrect value
        return Level.INFO;
    }

    private static void setLogLevels(Level logLevel) {
        LOGGING_MANAGER.setLogLevel(logLevel);
    }

    @SuppressWarnings({ "EmptyMethod" })
    @Pointcut("@annotation(logging)")
    public void callAt(Logging logging) {
        // Pointcut method - body intentionally empty
    }

    /**
     * Main method of the aspect
     */
    @Around(value = "callAt(logging) && execution(@Logging * *.*(..))", argNames = "pjp,logging")
    public Object around(ProceedingJoinPoint pjp,
            Logging logging) throws Throwable {

        boolean isOnRequestHandler = placedOnRequestHandler(pjp);
        boolean isOnRequestStreamHandler = placedOnStreamHandler(pjp);

        setLogLevelBasedOnSamplingRate(pjp, logging);
        addLambdaContextToLoggingContext(pjp);
        getXrayTraceId().ifPresent(xRayTraceId -> MDC.put(FUNCTION_TRACE_ID.getName(), xRayTraceId));

        Object[] proceedArgs = logEvent(pjp, logging, isOnRequestHandler, isOnRequestStreamHandler);

        if (!logging.correlationIdPath().isEmpty()) {
            captureCorrelationId(logging.correlationIdPath(), proceedArgs, isOnRequestHandler,
                    isOnRequestStreamHandler);
        }

        OutputStream backupOutputStream = null;
        if (isOnRequestStreamHandler) {
            // To log the result of a RequestStreamHandler (OutputStream), we need to do the following:
            // 1. backup a reference to the OutputStream provided by Lambda
            // 2. create a temporary OutputStream and pass it to the handler method
            // 3. retrieve this temporary stream to log it (if enabled)
            // 4. write it back to the OutputStream provided by Lambda
            backupOutputStream = prepareOutputStreamForLogging(logging, proceedArgs);
        }

        Object lambdaFunctionResponse;
        try {
            lambdaFunctionResponse = pjp.proceed(proceedArgs);
        } catch (Throwable t) {
            handleException(pjp, logging, t);
            throw t;
        } finally {
            performCleanup(logging);
        }

        logResponse(pjp, logging, lambdaFunctionResponse, isOnRequestHandler, isOnRequestStreamHandler,
                backupOutputStream, proceedArgs);

        return lambdaFunctionResponse;
    }

    private Object[] logEvent(ProceedingJoinPoint pjp, Logging logging,
            boolean isOnRequestHandler, boolean isOnRequestStreamHandler) {
        Object[] proceedArgs = pjp.getArgs();

        if (logging.logEvent() || POWERTOOLS_LOG_EVENT) {
            if (isOnRequestHandler) {
                logRequestHandlerEvent(pjp, pjp.getArgs()[0]);
            } else if (isOnRequestStreamHandler) {
                proceedArgs = logRequestStreamHandlerEvent(pjp);
            }
        }
        return proceedArgs;
    }

    private void addLambdaContextToLoggingContext(ProceedingJoinPoint pjp) {
        Context extractedContext = extractContext(pjp);

        if (extractedContext != null) {
            PowertoolsLoggedFields.setValuesFromLambdaContext(extractedContext).forEach(MDC::put);
            MDC.put(FUNCTION_COLD_START.getName(), isColdStart() ? "true" : "false");
            MDC.put(SERVICE.getName(), serviceName());
        }
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

            MDC.put(PowertoolsLoggedFields.SAMPLING_RATE.getName(), String.valueOf(samplingRate));

            if (samplingRate == 0) {
                return;
            }

            float sample = SAMPLER.nextFloat();

            if (samplingRate > sample) {
                setLogLevels(Level.DEBUG);

                LOG.debug("Changed log level to DEBUG based on Sampling configuration. "
                        + "Sampling Rate: {}, Sampler Value: {}.", samplingRate, sample);
            } else if (LEVEL_AT_INITIALISATION != LOGGING_MANAGER.getLogLevel(LOG)) {
                setLogLevels(LEVEL_AT_INITIALISATION);
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

    @SuppressWarnings("java:S3457")
    private void logRequestHandlerEvent(final ProceedingJoinPoint pjp, final Object event) {
        Logger log = logger(pjp);
        if (log.isInfoEnabled()) {
            log.info("Handler Event", entry("event", event));
        }
    }

    @SuppressWarnings("java:S3457")
    private Object[] logRequestStreamHandlerEvent(final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        Logger log = logger(pjp);
        if (log.isInfoEnabled()) {
            try {
                byte[] bytes = bytesFromInputStreamSafely((InputStream) pjp.getArgs()[0]);
                args[0] = new ByteArrayInputStream(bytes);
                // do not log asJson as it can be something else (String, XML...)
                log.info("Handler Event", entry("event", new String(bytes, UTF_8)));
            } catch (IOException e) {
                LOG.warn("Failed to log event from supplied input stream.", e);
            }
        }
        return args;
    }

    @SuppressWarnings("java:S3457")
    private void logRequestHandlerResponse(final ProceedingJoinPoint pjp, final Object response) {
        Logger log = logger(pjp);
        if (log.isInfoEnabled()) {
            log.info("Handler Response", entry("response", response));
        }
    }

    @SuppressWarnings("java:S3457")
    private void logRequestStreamHandlerResponse(final ProceedingJoinPoint pjp, final byte[] bytes) {
        Logger log = logger(pjp);
        if (log.isInfoEnabled()) {
            // we do not log with asJson as it can be something else (String, XML, ...)
            log.info("Handler Response", entry("response", new String(bytes, UTF_8)));
        }
    }

    private void captureCorrelationId(final String correlationIdPath,
            Object[] proceedArgs,
            final boolean isOnRequestHandler,
            final boolean isOnRequestStreamHandler) {
        if (isOnRequestHandler) {
            JsonNode jsonNode = JsonConfig.get().getObjectMapper().valueToTree(proceedArgs[0]);
            setCorrelationIdFromNode(correlationIdPath, jsonNode);
        } else if (isOnRequestStreamHandler) {
            try {
                byte[] bytes = bytesFromInputStreamSafely((InputStream) proceedArgs[0]);
                JsonNode jsonNode = JsonConfig.get().getObjectMapper().readTree(bytes);
                proceedArgs[0] = new ByteArrayInputStream(bytes);

                setCorrelationIdFromNode(correlationIdPath, jsonNode);
            } catch (IOException e) {
                LOG.warn("Failed to capture correlation id on event from supplied input stream.", e);
            }
        }
    }

    private void setCorrelationIdFromNode(String correlationIdPath, JsonNode jsonNode) {
        Expression<JsonNode> jmesExpression = JsonConfig.get().getJmesPath().compile(correlationIdPath);
        JsonNode node = jmesExpression.search(jsonNode);

        String asText = node.asText();
        if (null != asText && !asText.isEmpty()) {
            MDC.put(CORRELATION_ID.getName(), asText);
        } else {
            LOG.warn("Unable to extract any correlation id. Is your function expecting supported event type?");
        }
    }

    private byte[] bytesFromInputStreamSafely(final InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
                OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8)) {
            int n;
            char[] buffer = new char[4096];
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }
            writer.flush();
            return out.toByteArray();
        }
    }

    private OutputStream prepareOutputStreamForLogging(Logging logging,
            Object[] proceedArgs) {
        if (logging.logResponse() || POWERTOOLS_LOG_RESPONSE) {
            OutputStream backupOutputStream = (OutputStream) proceedArgs[1];
            proceedArgs[1] = new ByteArrayOutputStream();
            return backupOutputStream;
        }
        return null;
    }

    private void handleException(ProceedingJoinPoint pjp, Logging logging, Throwable t) {
        if (LOGGING_MANAGER instanceof BufferManager) {
            if (logging.flushBufferOnUncaughtError()) {
                ((BufferManager) LOGGING_MANAGER).flushBuffer();
            } else {
                ((BufferManager) LOGGING_MANAGER).clearBuffer();
            }
        }
        if (logging.logError() || POWERTOOLS_LOG_ERROR) {
            logger(pjp).error(MarkerFactory.getMarker("FATAL"), "Exception in Lambda Handler", t);
        }
    }

    private void performCleanup(Logging logging) {
        if (logging.clearState()) {
            MDC.clear();
        }
        if (LOGGING_MANAGER instanceof BufferManager) {
            ((BufferManager) LOGGING_MANAGER).clearBuffer();
        }
        coldStartDone();
    }

    private void logResponse(ProceedingJoinPoint pjp, Logging logging, Object lambdaFunctionResponse,
            boolean isOnRequestHandler, boolean isOnRequestStreamHandler,
            OutputStream backupOutputStream, Object[] proceedArgs) throws IOException {
        if (logging.logResponse() || POWERTOOLS_LOG_RESPONSE) {
            if (isOnRequestHandler) {
                logRequestHandlerResponse(pjp, lambdaFunctionResponse);
            } else if (isOnRequestStreamHandler && backupOutputStream != null) {
                byte[] bytes = ((ByteArrayOutputStream) proceedArgs[1]).toByteArray();
                logRequestStreamHandlerResponse(pjp, bytes);
                backupOutputStream.write(bytes);
            }
        }
    }

    private Logger logger(final ProceedingJoinPoint pjp) {
        return LoggerFactory.getLogger(pjp.getSignature().getDeclaringType());
    }
}
