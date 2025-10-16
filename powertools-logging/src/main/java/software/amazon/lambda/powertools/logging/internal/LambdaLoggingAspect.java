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
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.extractContext;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnRequestHandler;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.placedOnStreamHandler;
import static software.amazon.lambda.powertools.logging.argument.StructuredArguments.entry;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_ERROR;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_EVENT;
import static software.amazon.lambda.powertools.logging.internal.LoggingConstants.POWERTOOLS_LOG_RESPONSE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.utilities.JsonConfig;

@Aspect
@DeclarePrecedence("*, software.amazon.lambda.powertools.logging.internal.LambdaLoggingAspect")
public final class LambdaLoggingAspect {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaLoggingAspect.class);
    private static final LoggingManager LOGGING_MANAGER;

    static {
        LOGGING_MANAGER = LoggingManagerRegistry.getLoggingManager();
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

        // Initialize logging using PowertoolsLogging
        Context context = extractContext(pjp);
        Object[] proceedArgs = pjp.getArgs();

        if (isHandlerMethod(pjp) && context != null) {
            Object event = null;
            if (!logging.correlationIdPath().isEmpty()) {
                if (isOnRequestHandler && proceedArgs.length > 0) {
                    event = proceedArgs[0];
                } else if (isOnRequestStreamHandler && proceedArgs.length > 0) {
                    try {
                        byte[] bytes = bytesFromInputStreamSafely((InputStream) proceedArgs[0]);
                        // Parse JSON string to Object for correlation ID extraction
                        event = JsonConfig.get().getObjectMapper().readTree(bytes);
                        proceedArgs[0] = new ByteArrayInputStream(bytes); // Restore stream
                    } catch (IOException e) {
                        LOG.warn("Failed to read event from input stream for correlation ID extraction.", e);
                    }
                }
            }

            PowertoolsLogging.initializeLogging(context, logging.samplingRate(),
                    logging.correlationIdPath().isEmpty() ? null : logging.correlationIdPath(), event);
        }

        logEvent(pjp, logging, isOnRequestHandler, isOnRequestStreamHandler, proceedArgs);

        @SuppressWarnings("PMD.CloseResource") // Lambda-owned stream, not ours to close
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
        } catch (Throwable t) { // NOPMD - AspectJ proceed() throws Throwable
            handleException(pjp, logging, t);
            throw t;
        } finally {
            PowertoolsLogging.clearState(logging.clearState());
        }

        logResponse(pjp, logging, lambdaFunctionResponse, isOnRequestHandler, isOnRequestStreamHandler,
                backupOutputStream, proceedArgs);

        return lambdaFunctionResponse;
    }

    private void logEvent(ProceedingJoinPoint pjp, Logging logging,
            boolean isOnRequestHandler, boolean isOnRequestStreamHandler, Object[] proceedArgs) {

        if (logging.logEvent() || POWERTOOLS_LOG_EVENT) {
            if (isOnRequestHandler) {
                logRequestHandlerEvent(pjp, proceedArgs[0]);
            } else if (isOnRequestStreamHandler) {
                logRequestStreamHandlerEvent(pjp, proceedArgs);
            }
        }
    }

    @SuppressWarnings("java:S3457")
    private void logRequestHandlerEvent(final ProceedingJoinPoint pjp, final Object event) {
        Logger log = logger(pjp);
        if (log.isInfoEnabled()) {
            log.info("Handler Event", entry("event", event));
        }
    }

    @SuppressWarnings("java:S3457")
    private void logRequestStreamHandlerEvent(final ProceedingJoinPoint pjp, Object[] args) {
        Logger log = logger(pjp);
        if (log.isInfoEnabled()) {
            try {
                byte[] bytes = bytesFromInputStreamSafely((InputStream) args[0]);
                args[0] = new ByteArrayInputStream(bytes);
                // do not log asJson as it can be something else (String, XML...)
                log.info("Handler Event", entry("event", new String(bytes, UTF_8)));
            } catch (IOException e) {
                LOG.warn("Failed to log event from supplied input stream.", e);
            }
        }
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
