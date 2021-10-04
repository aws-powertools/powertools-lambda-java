/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.logging.LoggingUtils.appendKey;
import static software.amazon.lambda.powertools.logging.LoggingUtils.appendKeys;
import static software.amazon.lambda.powertools.logging.LoggingUtils.objectMapper;

@Aspect
@DeclarePrecedence("*, LambdaLoggingAspect")
public final class LambdaLoggingAspect {
    private static final Logger LOG = LogManager.getLogger(LambdaLoggingAspect.class);
    private static final Random SAMPLER = new Random();

    private static final String LOG_LEVEL = System.getenv("POWERTOOLS_LOG_LEVEL");
    private static final String SAMPLING_RATE = System.getenv("POWERTOOLS_LOGGER_SAMPLE_RATE");

    private static Level LEVEL_AT_INITIALISATION;

    static {
        if (null != LOG_LEVEL) {
            resetLogLevels(Level.getLevel(LOG_LEVEL));
        }

        LEVEL_AT_INITIALISATION = LOG.getLevel();
    }

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(logging)")
    public void callAt(Logging logging) {
    }

    @Around(value = "callAt(logging) && execution(@Logging * *.*(..))", argNames = "pjp,logging")
    public Object around(ProceedingJoinPoint pjp,
                         Logging logging) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        setLogLevelBasedOnSamplingRate(pjp, logging);

        Context extractedContext = extractContext(pjp);

        if(null != extractedContext) {
            appendKeys(DefaultLambdaFields.values(extractedContext));
            appendKey("coldStart", isColdStart() ? "true" : "false");
            appendKey("service", serviceName());
        }

        getXrayTraceId().ifPresent(xRayTraceId -> appendKey("xray_trace_id", xRayTraceId));

        if (logging.logEvent()) {
            proceedArgs = logEvent(pjp);
        }

        if (!logging.correlationIdPath().isEmpty()) {
            proceedArgs = captureCorrelationId(logging.correlationIdPath(), pjp);
        }

        Object proceed = pjp.proceed(proceedArgs);

        if(logging.clearState()) {
            ThreadContext.clearMap();
        }

        coldStartDone();
        return proceed;
    }

    private static void resetLogLevels(Level logLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), logLevel);
        ctx.updateLoggers();
    }

    private void setLogLevelBasedOnSamplingRate(final ProceedingJoinPoint pjp,
                                                final Logging logging) {
        double samplingRate = samplingRate(logging);

        if (isHandlerMethod(pjp)) {

            if (samplingRate < 0 || samplingRate > 1) {
                LOG.debug("Skipping sampling rate configuration because of invalid value. Sampling rate: {}", samplingRate);
                return;
            }

            appendKey("samplingRate", String.valueOf(samplingRate));

            if (samplingRate == 0) {
                return;
            }

            float sample = SAMPLER.nextFloat();

            if (samplingRate > sample) {
                resetLogLevels(Level.DEBUG);

                LOG.debug("Changed log level to DEBUG based on Sampling configuration. " +
                        "Sampling Rate: {}, Sampler Value: {}.", samplingRate, sample);
            } else if (LEVEL_AT_INITIALISATION != LOG.getLevel()) {
                resetLogLevels(LEVEL_AT_INITIALISATION);
            }
        }
    }

    private double samplingRate(final Logging logging) {
        if (null != SAMPLING_RATE) {
            try {
                return Double.parseDouble(SAMPLING_RATE);
            } catch (NumberFormatException e) {
                LOG.debug("Skipping sampling rate on environment variable configuration because of invalid " +
                        "value. Sampling rate: {}", SAMPLING_RATE);
            }
        }
        return logging.samplingRate();
    }

    private Object[] logEvent(final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        if (isHandlerMethod(pjp)) {
            if (placedOnRequestHandler(pjp)) {
                Logger log = logger(pjp);
                asJson(pjp, pjp.getArgs()[0])
                        .ifPresent(log::info);
            }

            if (placedOnStreamHandler(pjp)) {
                args = logFromInputStream(pjp);
            }
        }

        return args;
    }

    private Object[] captureCorrelationId(final String correlationIdPath,
                                          final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (isHandlerMethod(pjp)) {
            if (placedOnRequestHandler(pjp)) {
                Object arg = pjp.getArgs()[0];
                JsonNode jsonNode = objectMapper().valueToTree(arg);

                setCorrelationIdFromNode(correlationIdPath, pjp, jsonNode);

                return args;
            }

            if (placedOnStreamHandler(pjp)) {
                try {
                    byte[] bytes = bytesFromInputStreamSafely((InputStream) pjp.getArgs()[0]);
                    JsonNode jsonNode = objectMapper().readTree(bytes);
                    args[0] = new ByteArrayInputStream(bytes);

                    setCorrelationIdFromNode(correlationIdPath, pjp, jsonNode);

                    return args;
                } catch (IOException e) {
                    Logger log = logger(pjp);
                    log.warn("Failed to capture correlation id on event from supplied input stream.", e);
                }
            }
        }

        return args;
    }

    private void setCorrelationIdFromNode(String correlationIdPath, ProceedingJoinPoint pjp, JsonNode jsonNode) {
        JsonNode node = jsonNode.at(JsonPointer.compile(correlationIdPath));

        String asText = node.asText();
        if (null != asText && !asText.isEmpty()) {
            LoggingUtils.setCorrelationId(asText);
        } else {
            logger(pjp).debug("Unable to extract any correlation id. Is your function expecting supported event type?");
        }
    }

    private Object[] logFromInputStream(final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        try {
            byte[] bytes = bytesFromInputStreamSafely((InputStream) pjp.getArgs()[0]);
            args[0] = new ByteArrayInputStream(bytes);
            Logger log = logger(pjp);

            asJson(pjp, objectMapper().readValue(bytes, Map.class))
                    .ifPresent(log::info);

        } catch (IOException e) {
            Logger log = logger(pjp);
            log.debug("Failed to log event from supplied input stream.", e);
        }

        return args;
    }

    private byte[] bytesFromInputStreamSafely(final InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, UTF_8)) {
            OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);

            IOUtils.copy(reader, writer);
            writer.flush();
            return out.toByteArray();
        }
    }

    private Optional<String> asJson(final ProceedingJoinPoint pjp,
                                    final Object target) {
        try {
            return ofNullable(objectMapper().writeValueAsString(target));
        } catch (JsonProcessingException e) {
            logger(pjp).error("Failed logging event of type {}", target.getClass(), e);
            return empty();
        }
    }

    private Logger logger(final ProceedingJoinPoint pjp) {
        return LogManager.getLogger(pjp.getSignature().getDeclaringType());
    }
}
