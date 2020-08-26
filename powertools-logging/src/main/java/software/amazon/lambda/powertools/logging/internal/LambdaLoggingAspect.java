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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.IOUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.logging.PowertoolsLogger.appendKey;
import static software.amazon.lambda.powertools.logging.PowertoolsLogger.appendKeys;

@Aspect
public final class LambdaLoggingAspect {
    private static final Logger LOG = LogManager.getLogger(LambdaLoggingAspect.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Random SAMPLER = new Random();

    private static final String LOG_LEVEL = System.getenv("LOG_LEVEL");
    private static final String SAMPLING_RATE = System.getenv("POWERTOOLS_LOGGER_SAMPLE_RATE");

    private static Level LEVEL_AT_INITIALISATION;

    static {
        if (null != LOG_LEVEL) {
            resetLogLevels(Level.getLevel(LOG_LEVEL));
        }

        LEVEL_AT_INITIALISATION = LOG.getLevel();
    }

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(powertoolsLogging)")
    public void callAt(PowertoolsLogging powertoolsLogging) {
    }

    @Around(value = "callAt(powertoolsLogging) && execution(@PowertoolsLogging * *.*(..))", argNames = "pjp,powertoolsLogging")
    public Object around(ProceedingJoinPoint pjp,
                         PowertoolsLogging powertoolsLogging) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        setLogLevelBasedOnSamplingRate(pjp, powertoolsLogging);

        extractContext(pjp)
                .ifPresent(context -> {
                    appendKeys(DefaultLambdaFields.values(context));
                    appendKey("coldStart", null == isColdStart() ? "true" : "false");
                    appendKey("service", serviceName());
                });


        if (powertoolsLogging.logEvent()) {
            proceedArgs = logEvent(pjp);
        }

        Object proceed = pjp.proceed(proceedArgs);

        coldStartDone();
        return proceed;
    }

    private static void resetLogLevels(Level logLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), logLevel);
        ctx.updateLoggers();
    }

    private void setLogLevelBasedOnSamplingRate(final ProceedingJoinPoint pjp,
                                                final PowertoolsLogging powertoolsLogging) {
        if (isHandlerMethod(pjp)) {
            float sample = SAMPLER.nextFloat();
            double samplingRate = samplingRate(powertoolsLogging);

            if (samplingRate < 0 || samplingRate > 1) {
                LOG.debug("Skipping sampling rate configuration because of invalid value. Sampling rate: {}", samplingRate);
                return;
            }

            appendKey("samplingRate", String.valueOf(samplingRate));

            if (samplingRate > sample) {
                resetLogLevels(Level.DEBUG);

                LOG.debug("Changed log level to DEBUG based on Sampling configuration. " +
                        "Sampling Rate: {}, Sampler Value: {}.", samplingRate, sample);
            } else if (LEVEL_AT_INITIALISATION != LOG.getLevel()) {
                resetLogLevels(LEVEL_AT_INITIALISATION);
            }
        }
    }

    private double samplingRate(final PowertoolsLogging powertoolsLogging) {
        if (null != SAMPLING_RATE) {
            try {
                return Double.parseDouble(SAMPLING_RATE);
            } catch (NumberFormatException e) {
                LOG.debug("Skipping sampling rate on environment variable configuration because of invalid " +
                        "value. Sampling rate: {}", SAMPLING_RATE);
            }
        }
        return powertoolsLogging.samplingRate();
    }

    private Optional<Context> extractContext(final ProceedingJoinPoint pjp) {

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

    private Object[] logEvent(final ProceedingJoinPoint pjp) {
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

    private Object[] logFromInputStream(final ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out);
             InputStreamReader reader = new InputStreamReader((InputStream) pjp.getArgs()[0])) {

            IOUtils.copy(reader, writer);
            writer.flush();
            byte[] bytes = out.toByteArray();
            args[0] = new ByteArrayInputStream(bytes);

            Logger log = logger(pjp);
            log.info(MAPPER.readValue(bytes, Map.class));

        } catch (IOException e) {
            Logger log = logger(pjp);
            log.debug("Failed to log event from supplied input stream.", e);
        }

        return args;
    }

    private Logger logger(final ProceedingJoinPoint pjp) {
        return LogManager.getLogger(pjp.getSignature().getDeclaringType());
    }
}
