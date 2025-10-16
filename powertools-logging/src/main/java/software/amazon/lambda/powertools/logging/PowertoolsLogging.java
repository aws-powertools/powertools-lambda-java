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

package software.amazon.lambda.powertools.logging;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.getXrayTraceId;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.LAMBDA_LOG_LEVEL;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_LEVEL;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_SAMPLING_RATE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_COLD_START;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.FUNCTION_TRACE_ID;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SAMPLING_RATE;
import static software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields.SERVICE;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;

import io.burt.jmespath.Expression;
import software.amazon.lambda.powertools.logging.internal.BufferManager;
import software.amazon.lambda.powertools.logging.internal.LoggingManager;
import software.amazon.lambda.powertools.logging.internal.LoggingManagerRegistry;
import software.amazon.lambda.powertools.logging.internal.PowertoolsLoggedFields;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * PowertoolsLogging provides a logging backend-agnostic API for managing Powertools logging functionality.
 * This class abstracts away the underlying logging framework (Log4j2, Logback) and provides a unified
 * interface for Lambda context extraction, correlation ID handling, sampling rate configuration,
 * log buffering operations, and other Lambda-specific logging features.
 * 
 * <p>This class serves as a programmatic alternative to AspectJ-based {@code @Logging} annotation,
 * allowing developers to integrate Powertools logging capabilities without AspectJ dependencies.</p>
 * 
 * Key features:
 * <ul>
 *   <li>Lambda context initialization with function metadata, trace ID, and service name</li>
 *   <li>Sampling rate configuration for DEBUG logging</li>
 *   <li>Backend-independent log buffer management (flush/clear operations)</li>
 *   <li>MDC state management for structured logging</li>
 * </ul>
 */
public final class PowertoolsLogging {
    private static final Logger LOG = LoggerFactory.getLogger(PowertoolsLogging.class);
    private static final Random SAMPLER = new Random();
    private static boolean hasBeenInitialized = false;

    static {
        initializeLogLevel();
    }

    private PowertoolsLogging() {
        // Utility class
    }

    private static void initializeLogLevel() {
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
            setLogLevel(powertoolsLevel);
        } else if (LAMBDA_LOG_LEVEL != null) {
            setLogLevel(getLevelFromString(LAMBDA_LOG_LEVEL));
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

    private static void setLogLevel(Level logLevel) {
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        loggingManager.setLogLevel(logLevel);
    }

    /**
     * Flushes the log buffer for the current Lambda execution.
     * This method will flush any buffered logs to the output stream.
     * The operation is backend-independent and works with both Log4j2 and Logback.
     */
    public static void flushBuffer() {
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof BufferManager) {
            ((BufferManager) loggingManager).flushBuffer();
        }
    }

    /**
     * Clears the log buffer for the current Lambda execution.
     * This method will discard any buffered logs without outputting them.
     * The operation is backend-independent and works with both Log4j2 and Logback.
     */
    public static void clearBuffer() {
        LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
        if (loggingManager instanceof BufferManager) {
            ((BufferManager) loggingManager).clearBuffer();
        }
    }

    /**
     * Initializes Lambda logging context with standard Powertools fields.
     * This method should be called at the beginning of your Lambda handler to set up
     * logging context with Lambda function information, trace ID, and service name.
     * 
     * @param context the Lambda context provided by AWS Lambda runtime
     */
    public static void initializeLogging(Context context) {
        initializeLogging(context, 0.0, null, null);
    }

    /**
     * Initializes Lambda logging context with sampling rate configuration.
     * This method sets up logging context and optionally enables DEBUG logging
     * based on the provided sampling rate.
     * 
     * @param context the Lambda context provided by AWS Lambda runtime
     * @param samplingRate sampling rate for DEBUG logging (0.0 to 1.0)
     */
    public static void initializeLogging(Context context, double samplingRate) {
        initializeLogging(context, samplingRate, null, null);
    }

    /**
     * Initializes Lambda logging context with correlation ID extraction.
     * This method sets up logging context and extracts correlation ID from the event
     * using the provided JSON path.
     * 
     * @param context the Lambda context provided by AWS Lambda runtime
     * @param correlationIdPath JSON path to extract correlation ID from event
     * @param event the Lambda event object
     */
    public static void initializeLogging(Context context, String correlationIdPath, Object event) {
        initializeLogging(context, 0.0, correlationIdPath, event);
    }

    /**
     * Initializes Lambda logging context with full configuration.
     * This method sets up logging context with Lambda function information,
     * configures sampling rate for DEBUG logging, and optionally extracts
     * correlation ID from the event.
     * 
     * @param context the Lambda context provided by AWS Lambda runtime
     * @param samplingRate sampling rate for DEBUG logging (0.0 to 1.0)
     * @param correlationIdPath JSON path to extract correlation ID from event (can be null)
     * @param event the Lambda event object (required if correlationIdPath is provided)
     */
    public static void initializeLogging(Context context, double samplingRate, String correlationIdPath, Object event) {
        if (hasBeenInitialized) {
            coldStartDone();
        }
        hasBeenInitialized = true;
        
        addLambdaContextToLoggingContext(context);
        setLogLevelBasedOnSamplingRate(samplingRate);
        getXrayTraceId().ifPresent(xRayTraceId -> MDC.put(FUNCTION_TRACE_ID.getName(), xRayTraceId));

        if (correlationIdPath != null && !correlationIdPath.isEmpty() && event != null) {
            captureCorrelationId(correlationIdPath, event);
        }
    }

    private static void addLambdaContextToLoggingContext(Context context) {
        if (context != null) {
            PowertoolsLoggedFields.setValuesFromLambdaContext(context).forEach(MDC::put);
            MDC.put(FUNCTION_COLD_START.getName(), isColdStart() ? "true" : "false");
            MDC.put(SERVICE.getName(), serviceName());
        }
    }

    private static void setLogLevelBasedOnSamplingRate(double samplingRate) {
        double effectiveSamplingRate = getEffectiveSamplingRate(samplingRate);

        if (effectiveSamplingRate < 0 || effectiveSamplingRate > 1) {
            LOG.warn("Skipping sampling rate configuration because of invalid value. Sampling rate: {}",
                    effectiveSamplingRate);
            return;
        }

        MDC.put(SAMPLING_RATE.getName(), String.valueOf(effectiveSamplingRate));

        if (effectiveSamplingRate == 0) {
            return;
        }

        float sample = SAMPLER.nextFloat();
        if (effectiveSamplingRate > sample) {
            LoggingManager loggingManager = LoggingManagerRegistry.getLoggingManager();
            loggingManager.setLogLevel(Level.DEBUG);
            LOG.debug(
                    "Changed log level to DEBUG based on Sampling configuration. Sampling Rate: {}, Sampler Value: {}.",
                    effectiveSamplingRate, sample);
        }
    }

    // The environment variable takes precedence over manually set sampling rate
    private static double getEffectiveSamplingRate(double samplingRate) {
        String envSampleRate = POWERTOOLS_SAMPLING_RATE;
        if (envSampleRate != null) {
            try {
                return Double.parseDouble(envSampleRate);
            } catch (NumberFormatException e) {
                LOG.warn(
                        "Skipping sampling rate on environment variable configuration because of invalid value. Sampling rate: {}",
                        envSampleRate);
            }
        }

        return samplingRate;
    }

    private static void captureCorrelationId(String correlationIdPath, Object event) {
        try {
            JsonNode jsonNode = JsonConfig.get().getObjectMapper().valueToTree(event);
            Expression<JsonNode> jmesExpression = JsonConfig.get().getJmesPath().compile(correlationIdPath);
            JsonNode node = jmesExpression.search(jsonNode);

            String asText = node.asText();
            if (asText != null && !asText.isEmpty()) {
                MDC.put(PowertoolsLoggedFields.CORRELATION_ID.getName(), asText);
            } else {
                LOG.warn("Unable to extract any correlation id. Is your function expecting supported event type?");
            }
        } catch (Exception e) {
            LOG.warn("Failed to capture correlation id from event.", e);
        }
    }

    /**
     * Clears MDC state and log buffer.
     * 
     * @param clearMdcState whether to clear MDC state
     */
    public static void clearState(boolean clearMdcState) {
        if (clearMdcState) {
            MDC.clear();
        }
        clearBuffer();
    }
}
